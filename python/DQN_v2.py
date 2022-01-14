# -*- coding: utf-8 -*-

import tensorflow.compat.v1 as tf
from flask import Flask, request
# 用不到gym環境，環境為OM2M
# from agent import Env, TestEnv
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation
import gym
from tqdm import tqdm
import time
import math
from gym import spaces
from gym.utils import seeding
import random
import pandas as pd
import requests
from collections import defaultdict
import tensorflow.compat.v1 as tf
tf.disable_v2_behavior()

np.random.seed(1)
tf.random.set_random_seed(1)

app = Flask(__name__)

# Initialize the environment of our problem
# num_protocols = 4
#protocols = ['COAP', 'MQTT', 'WebSocket', 'XMPP']

# average delay(ms) of data bytes between 100、500、1000、1500 event-driven in packet loss rate 0 %  means
# coap:26.705  mqtt:82.5635  ws:105.82  xmpp:96.9525

# get data from gscl and return action based on test_update


"""
This part of code is the DQN brain, which is a brain of the agent.
All decisions are made in here.
Using Tensorflow to build the neural network.

View more on my tutorial page: https://morvanzhou.github.io/tutorials/

Using:
Tensorflow: 2.7.0
gym: 0.19.0
"""


# Deep Q Network off-policy
class DQN:
    def __init__(
            self,
            n_actions,
            n_features,
            learning_rate=0.01,
            reward_decay=0.9,
            e_greedy=0.9,
            replace_target_iter=300,
            memory_size=500,
            batch_size=32,
            e_greedy_increment=None,
            output_graph=False,
    ):
        self.step = 0
        self.n_actions = n_actions
        self.n_features = n_features
        self.lr = learning_rate
        self.gamma = reward_decay
        self.epsilon_max = e_greedy
        self.replace_target_iter = replace_target_iter
        self.memory_size = memory_size
        self.batch_size = batch_size
        self.epsilon_increment = e_greedy_increment
        self.epsilon = 0 if e_greedy_increment is not None else self.epsilon_max

        # total learning step
        self.learn_step_counter = 0

        # initialize zero memory [s, a, r, s_]
        # n_features * 2 <=> s1's n_features + s2's n_features
        # +2 <=> reward + action
        self.memory = np.zeros((self.memory_size, n_features*2+2))

        # consist of [target_net, evaluate_net]
        self._build_net()
        t_params = tf.get_collection('target_net_params')
        e_params = tf.get_collection('eval_net_params')
        self.replace_target_op = [tf.assign(t, e)
                                  for t, e in zip(t_params, e_params)]

        self.sess = tf.Session()

        if output_graph:
            # $ tensorboard --logdir=logs
            # tf.train.SummaryWriter soon be deprecated, use following
            tf.summary.FileWriter("logs/", self.sess.graph)

        self.sess.run(tf.global_variables_initializer())
        self.cost_his = []

    def _build_net(self):
        # ------------------ build evaluate_net ------------------
        self.s = tf.placeholder(
            tf.float32, [None, self.n_features], name='s')  # input
        self.q_target = tf.placeholder(
            tf.float32, [None, self.n_actions], name='Q_target')  # for calculating loss
        with tf.variable_scope('eval_net'):
            # c_names(collections_names) are the collections to store variables
            c_names, n_l1, w_initializer, b_initializer = \
                ['eval_net_params', tf.GraphKeys.GLOBAL_VARIABLES], 10, \
                tf.random_normal_initializer(
                    0., 0.3), tf.constant_initializer(0.1)  # config of layers

            # first layer. collections is used later when assign to target net
            with tf.variable_scope('l1'):
                w1 = tf.get_variable(
                    'w1', [self.n_features, n_l1], initializer=w_initializer, collections=c_names)
                b1 = tf.get_variable(
                    'b1', [1, n_l1], initializer=b_initializer, collections=c_names)
                l1 = tf.nn.relu(tf.matmul(self.s, w1) + b1)

            # second layer. collections is used later when assign to target net
            with tf.variable_scope('l2'):
                w2 = tf.get_variable(
                    'w2', [n_l1, self.n_actions], initializer=w_initializer, collections=c_names)
                b2 = tf.get_variable(
                    'b2', [1, self.n_actions], initializer=b_initializer, collections=c_names)
                self.q_eval = tf.matmul(l1, w2) + b2

        with tf.variable_scope('loss'):
            self.loss = tf.reduce_mean(
                tf.squared_difference(self.q_target, self.q_eval))
        with tf.variable_scope('train'):
            self._train_op = tf.train.RMSPropOptimizer(
                self.lr).minimize(self.loss)

        # ------------------ build target_net ------------------
        self.s_ = tf.placeholder(
            tf.float32, [None, self.n_features], name='s_')    # input
        with tf.variable_scope('target_net'):
            # c_names(collections_names) are the collections to store variables
            c_names = ['target_net_params', tf.GraphKeys.GLOBAL_VARIABLES]

            # first layer. collections is used later when assign to target net
            with tf.variable_scope('l1'):
                w1 = tf.get_variable(
                    'w1', [self.n_features, n_l1], initializer=w_initializer, collections=c_names)
                b1 = tf.get_variable(
                    'b1', [1, n_l1], initializer=b_initializer, collections=c_names)
                l1 = tf.nn.relu(tf.matmul(self.s_, w1) + b1)

            # second layer. collections is used later when assign to target net
            with tf.variable_scope('l2'):
                w2 = tf.get_variable(
                    'w2', [n_l1, self.n_actions], initializer=w_initializer, collections=c_names)
                b2 = tf.get_variable(
                    'b2', [1, self.n_actions], initializer=b_initializer, collections=c_names)
                self.q_next = tf.matmul(l1, w2) + b2

    def store_transition(self, s, a, r, s_):
        if not hasattr(self, 'memory_counter'):
            self.memory_counter = 0
        transition = np.hstack((s, [a, r], s_))

        # replace the old memory with new memory
        index = self.memory_counter % self.memory_size
        self.memory[index, :] = transition

        self.memory_counter += 1

    def choose_action(self, observation):
        # to have batch dimension when feed into tf placeholder
        observation = observation[np.newaxis, :]

        if np.random.uniform() < self.epsilon:
            # forward feed the observation and get q value for every actions
            actions_value = self.sess.run(
                self.q_eval, feed_dict={self.s: observation})
            action = np.argmax(actions_value)
        else:
            action = np.random.randint(0, self.n_actions)
        return action

    def learn(self):
        # check to replace target parameters
        if self.learn_step_counter % self.replace_target_iter == 0:
            self.sess.run(self.replace_target_op)
            print('\ntarget_params_replaced\n')

        # sample batch memory from all memory
        if self.memory_counter > self.memory_size:
            sample_index = np.random.choice(
                self.memory_size, size=self.batch_size)
        else:
            sample_index = np.random.choice(
                self.memory_counter, size=self.batch_size)
        batch_memory = self.memory[sample_index, :]

        q_next, q_eval = self.sess.run(
            [self.q_next, self.q_eval],
            feed_dict={
                self.s_: batch_memory[:, -self.n_features:],  # fixed params
                self.s: batch_memory[:, :self.n_features],  # newest params
            })

        # change q_target w.r.t q_eval's action
        q_target = q_eval.copy()

        batch_index = np.arange(self.batch_size, dtype=np.int32)
        eval_act_index = batch_memory[:, self.n_features].astype(int)
        reward = batch_memory[:, self.n_features + 1]

        q_target[batch_index, eval_act_index] = reward + \
            self.gamma * np.max(q_next, axis=1)

        """
        For example in this batch I have 2 samples and 3 actions:
        q_eval =
        [[1, 2, 3],
         [4, 5, 6]]

        q_target = q_eval =
        [[1, 2, 3],
         [4, 5, 6]]

        Then change q_target with the real q_target value w.r.t the q_eval's action.
        For example in:
            sample 0, I took action 0, and the max q_target value is -1;
            sample 1, I took action 2, and the max q_target value is -2:
        q_target =
        [[-1, 2, 3],
         [4, 5, -2]]

        So the (q_target - q_eval) becomes:
        [[(-1)-(1), 0, 0],
         [0, 0, (-2)-(6)]]

        We then backpropagate this error w.r.t the corresponding action to network,
        leave other action as error=0 cause we didn't choose it.
        """

        # train eval network
        _, self.cost = self.sess.run([self._train_op, self.loss],
                                     feed_dict={self.s: batch_memory[:, :self.n_features],
                                                self.q_target: q_target})
        self.cost_his.append(self.cost)

        # increasing epsilon
        self.epsilon = self.epsilon + \
            self.epsilon_increment if self.epsilon < self.epsilon_max else self.epsilon_max
        self.learn_step_counter += 1

    def plot_cost(self):
        import matplotlib.pyplot as plt
        plt.plot(np.arange(len(self.cost_his)), self.cost_his)
        plt.ylabel('Cost')
        plt.xlabel('training steps')
        plt.show()


agent = DQN(4, 2, learning_rate=0.01,
            reward_decay=0.9,
            e_greedy=0.9,
            replace_target_iter=200,
            memory_size=2000,
            output_graph=True
            )

# store 1000 data avoid buffer too large


# def def_value():
#     return np.NAN


record = dict()

# state and its chosen action
sadict = dict()

# def change_state_range(size):
#     # base 1500
#     # max  3500
#     return 1+(size-1500)//100


# define init state
# state = [data size , avg loss rate before this time]
# idx for the state order
# action = np.int64(0)
# s=[]
# immreward=0


# observation in maze
# (2,)                      shape
# [-0.5 -0.5]               observation
# <class 'numpy.ndarray'>   type

# modified observation
# [[0.25 0.  ]]
# (1, 2)
# <class 'numpy.ndarray'>


# transition in memory
# transition = np.hstack((s, [a, r], s_))
# [ 0.25 -0.5   1.    0.    0.25 -0.25]
# <class 'numpy.ndarray'>
# (6,)

# memory
# [[0. 0. 0. 0. 0. 0.]
#  [0. 0. 0. 0. 0. 0.]
#  [0. 0. 0. 0. 0. 0.]
#  ...
#  [0. 0. 0. 0. 0. 0.]
#  [0. 0. 0. 0. 0. 0.]
#  [0. 0. 0. 0. 0. 0.]]
# (2000, 6)
# <class 'tuple'>


@app.route('/get_data_size_return_action', methods=['POST'])
def get_data_size_return_action():

    # data[0]=datasize,data[1]=idx  for next state
    data = request.data.decode().split('//')
    datasize = float(data[0])
    idx = int(data[1])

    if idx == 500:
        agent.step += 1

    # fill in record
    # 5000 idx divide into 100 groups  ==> 5000/100 50個一組
    # 3000 is enough or it might loss
    # now protocol
    # <class 'numpy.ndarray'>
    record[idx % 500] = np.array([datasize, idx])
    # global action,s,immreward
    # learn agent from state before
    # if idx!=0 and idx+1==float(data[1]):
    #     s_=[change_state_range(float(data[0])),0.0]
    #     agent.learn(str(s),action,immreward,str(s_))
    # state = [data size , avg loss rate before this time]
    # s=[change_state_range(size),0.0]

    # action = agent.choose_action(str(s))
    # action = agent.choose_action(str(record[idx % 500]))
    action = agent.choose_action(record[idx % 500])
    sadict[np.array_str(record[idx % 500])] = action
    print('--------------***********************------------------')
    print('--------------***********************------------------')
    print('--------------***********************------------------')
    print('--------------***********************------------------')
    print('--------------***********************------------------')
    print('--------------action------------------')
    print(action)
    print('--------------***********************------------------')
    print('--------------***********************------------------')
    print('--------------***********************------------------')
    print('--------------***********************------------------')
    print('--------------***********************------------------')
    # print('..', size)
    # print("ok")
    # print('--------------***********************------------------')
    # print(action)
    # print('--------------***********************------------------')
    # same used no post

    if action == 0:
        print("coap")
        return "coap"
    if action == 1:
        print("mqtt")
        return "mqtt"
    if action == 2:
        print("ws")
        return "ws"
    if action == 3:
        print("xmpp")
        return "xmpp"

# update reward
# not returning anything


@app.route('/receive_delay_as_reward', methods=['POST'])
def receive_action_and_delay_as_reward():
    # global idx,immreward
    # state s_ is new observation
    # set immediate reward as delay/data_size
    data = request.data.decode().split('//')
    delay = float(data[0])
    idx = int(data[1])

    # order比number越大越好

    # s.shape = (2,1)
    s = s_ = record[idx % 500]
    a = sadict[np.array_str(s)]
    r = - delay
    if ((1+idx) % 500) in record:
        # get state by idx
        s_ = record[(1+idx) % 500]
    agent.store_transition(s, a, r, s_)
    if agent.step > 300 and agent.step % 5 == 0:
        agent.learn()
    s = s_
    # agent.learn(str(s), a, r, str(s_))

    # res = data.split('//')
    # res[0]=delay
    # res[1]=protocol

    # print(res[0])
    # print(res[1])
    # imm_reward = res[0]/res[1]
    return ""


if __name__ == '__main__':
    # app.debug = True
    app.run(host='140.116.247.69', port=9000)
