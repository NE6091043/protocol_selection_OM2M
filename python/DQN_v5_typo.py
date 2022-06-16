# -*- coding: utf-8 -*-
import requests
import threading
import matplotlib.pyplot as plt
import tensorflow.compat.v1 as tf
import time
from flask import Flask, request
# 用不到gym環境，環境為OM2M
# from agent import Env, TestEnv
import numpy as np
import os
tf.disable_v2_behavior()

np.random.seed(1)
tf.random.set_random_seed(1)

app = Flask(__name__)

min_D, max_D = 0, 100
min_val_delay, max_val_delay = 0.00002, 1.0
min_val_eff, max_val_eff = 0.0, 1.0

alpha, beta = 0.5, 0.5

app = Flask(__name__)


class DQN:
    def __init__(
            self,
            # 幾個actions
            n_actions,
            # 比如長寬高 就是3個features
            n_features,
            learning_rate=0.01,
            # gamma
            reward_decay=0.9,
            e_greedy=0.9,
            # 多少不步把eval network參數更新到target network
            replace_target_iter=300,
            memory_size=500,
            # 神經網路提升時 有stochastic gradient descent
            # 也就是隨機梯度下降
            batch_size=32,
            # 這邊我自己調epislon
            # e_greedy_increment=None,
            output_graph=False,
    ):
        self.n_actions = n_actions
        self.n_features = n_features
        self.lr = learning_rate
        self.gamma = reward_decay
        self.epsilon = e_greedy
        self.replace_target_iter = replace_target_iter
        self.memory_size = memory_size
        self.batch_size = batch_size
        # self.epsilon_increment = e_greedy_increment
        # self.epsilon = 0 if e_greedy_increment is not None else self.epsilon_max

        self.step = 0
        # self.decay = 0.999
        # self.min_egreedy = 0.05

        # self.action = 1

        # total learning step
        # learning時紀錄總共學習多少步
        # epislon會根據counter不斷提高
        self.learn_step_counter = 0

        # initialize zero memory [s, a, r, s_]
        # n_features * 2 <=> s1's n_features + s2's n_features
        # +2 <=> reward + action
        # memory先建立全0數組
        # memorysize => 比如200條記憶
        # 每條長度 : 比如2個觀測值*2(s跟s_)
        # action 一個
        # reward 一個
        self.memory = np.zeros((self.memory_size, n_features*2+2))

        # consist of [target_net, evaluate_net]
        self._build_net()
        t_params = tf.get_collection(
            tf.GraphKeys.GLOBAL_VARIABLES, scope='target_net')
        e_params = tf.get_collection(
            tf.GraphKeys.GLOBAL_VARIABLES, scope='eval_net')
        with tf.variable_scope('hard_replacement'):
            self.target_replace_op = [
                tf.assign(t, e) for t, e in zip(t_params, e_params)]

        self.sess = tf.Session()
        self.load_model("0504.ckpt")

        if output_graph:
            # $ tensorboard --logdir=logs
            # tf.train.SummaryWriter soon be deprecated, use following
            # 輸出tensorboard
            tf.summary.FileWriter("logs/", self.sess.graph)

        self.sess.run(tf.global_variables_initializer())
        self.cost_his = []

    def _build_net(self):
        # ------------------ all inputs ------------------------
        self.s = tf.placeholder(
            tf.float32, [None, self.n_features], name='s')  # input State
        self.s_ = tf.placeholder(
            tf.float32, [None, self.n_features], name='s_')  # input Next State
        self.r = tf.placeholder(tf.float32, [None, ], name='r')  # input Reward
        self.a = tf.placeholder(tf.int32, [None, ], name='a')  # input Action

        w_initializer, b_initializer = tf.random_normal_initializer(
            0., 0.3), tf.constant_initializer(0.1)

        # ------------------ build evaluate_net ------------------
        with tf.variable_scope('eval_net'):
            e1 = tf.layers.dense(self.s, 20, tf.nn.relu, kernel_initializer=w_initializer,
                                 bias_initializer=b_initializer, name='e1')
            self.q_eval = tf.layers.dense(e1, self.n_actions, kernel_initializer=w_initializer,
                                          bias_initializer=b_initializer, name='q')

        # ------------------ build target_net ------------------
        with tf.variable_scope('target_net'):
            t1 = tf.layers.dense(self.s_, 20, tf.nn.relu, kernel_initializer=w_initializer,
                                 bias_initializer=b_initializer, name='t1')
            self.q_next = tf.layers.dense(t1, self.n_actions, kernel_initializer=w_initializer,
                                          bias_initializer=b_initializer, name='t2')

        with tf.variable_scope('q_target'):
            q_target = self.r + self.gamma * \
                tf.reduce_max(self.q_next, axis=1,
                              name='Qmax_s_')    # shape=(None, )
            self.q_target = tf.stop_gradient(q_target)
        with tf.variable_scope('q_eval'):
            a_indices = tf.stack(
                [tf.range(tf.shape(self.a)[0], dtype=tf.int32), self.a], axis=1)
            self.q_eval_wrt_a = tf.gather_nd(
                params=self.q_eval, indices=a_indices)    # shape=(None, )
        with tf.variable_scope('loss'):
            self.loss = tf.reduce_mean(tf.squared_difference(
                self.q_target, self.q_eval_wrt_a, name='TD_error'))
        with tf.variable_scope('train'):
            self._train_op = tf.train.RMSPropOptimizer(
                self.lr).minimize(self.loss)

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
        # observation本來為一維數據
        # 為了使tf能處理把維度增加一個
        # 變成二維數據
        observation = observation[np.newaxis, :]

        if np.random.uniform() < self.epsilon:
            # forward feed the observation and get q value for every actions
            actions_value = self.sess.run(
                self.q_eval, feed_dict={self.s: observation})
            # if str(observation) == '[[2360.    0.]]':
            print('--------------***********************------------------')
            print('--------------***********************------------------')
            print('--------------***********************------------------')
            print('--------------***********************------------------')
            print('--------------***********************------------------')
            print(actions_value)
            print('--------------***********************------------------')
            print('--------------***********************------------------')
            print('--------------***********************------------------')
            print('--------------***********************------------------')
            print('--------------***********************------------------')
            action = np.argmax(actions_value)
        else:
            action = np.random.randint(0, self.n_actions)
        return action

    def learn(self):
        # check to replace target parameters
        if self.learn_step_counter % self.replace_target_iter == 0:
            self.sess.run(self.target_replace_op)
            print('\ntarget_params_replaced\n')

        # sample batch memory from all memory
        if self.memory_counter > self.memory_size:
            sample_index = np.random.choice(
                self.memory_size, size=self.batch_size)
        else:
            sample_index = np.random.choice(
                self.memory_counter, size=self.batch_size)
        batch_memory = self.memory[sample_index, :]

        _, cost = self.sess.run(
            [self._train_op, self.loss],
            feed_dict={
                self.s: batch_memory[:, :self.n_features],
                self.a: batch_memory[:, self.n_features],
                self.r: batch_memory[:, self.n_features + 1],
                self.s_: batch_memory[:, -self.n_features:],
            })

        self.cost_his.append(cost)

        # increasing epsilon
        self.epsilon = self.epsilon + \
            self.epsilon_increment if self.epsilon < self.epsilon_max else self.epsilon_max
        self.learn_step_counter += 1

    def plot_loss(self):
        plt.plot(np.arange(len(self.cost_his)), self.cost_his)
        plt.ylabel('Cost')
        plt.xlabel('training steps')
        plt.show()

    def saved_model(self, filename):
        tf.train.Saver().save(self.sess, filename)

    def load_model(self, filename):
        exist = os.path.exists(filename)
        if not exist:
            return
        tf.train.Saver().restore(self.sess, filename)


# action,features
# 4,4(protocol,size,loss,bandwidth)
agent = DQN(4, 4, e_greedy=0.9,
            replace_target_iter=200,
            memory_size=2000,
            output_graph=True
            )


# store 1000 data avoid buffer too large
# store_transition(self, s, a, r, s_)

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

def normalize(val, delay):
    res = 0.0
    if delay == 1:
        if max_val_delay == min_val_delay:
            res = 50
        else:
            res = 100*(50000-val)/(50000-1)
    else:
        if max_val_eff == min_val_eff:
            res = 50
        else:
            res = 100*(val-min_val_eff)/(max_val_eff-min_val_eff)
    return round(res, 5)


reward = 1.0
st_0 = [1, 0, 0, 0]
st_1 = [1, 1000, 0, 500]
at_0 = 1
prevdatasize, prevloss, prevbandwdth = 1000, 0, 500
flag = False


@app.route('/state_post', methods=['POST'])
def state_post():
    global st_0, st_1, at_0
    global reward, flag
    data = request.data.decode().split('//')
    datasize = int(data[0])
    loss_rate = int(data[1])
    bandwidth = int(data[2])
    st_1 = [at_0, datasize, loss_rate, bandwidth]
    if flag == False:
        reward = -1000
    if agent.step == 0:
        agent.store_transition(np.array(st_0), at_0, reward, np.array(st_1))
    agent.step += 1
    if agent.step >= 50 and agent.step % 5 == 0:
        agent.learn()
    flag = False
    at_0 = agent.choose_action(np.array(st_1))
    st_0[0] = at_0
    st_0[1] = loss_rate
    st_0[2] = bandwidth
    st_0[3] = bandwidth
    if at_0 == 0:
        print("coap")
        return "coap"
    if at_0 == 1:
        print("mqtt")
        return "mqtt"
    if at_0 == 2:
        print("ws")
        return "ws"
    if at_0 == 3:
        print("xmpp")
        return "xmpp"
    return ""


@ app.route('/receive_reward', methods=['POST'])
def receive_reward():
    global reward, flag
    global alpha, beta
    delay, effi = request.data.decode().split('//')
    reward = alpha*normalize(float(delay), 1)+beta*normalize(float(effi), 0)
    flag = True
    return ""


begin_plot = 0


def shutdown_server():
    func = request.environ.get('werkzeug.server.shutdown')
    if func is None:
        raise RuntimeError('Not running with the Werkzeug Server')
    func()


@ app.route('/shutdown', methods=['POST'])
def shutdown():
    global begin_plot
    plot = request.data.decode()
    if plot == "1":
        begin_plot = 1
    shutdown_server()
    return 'Server shutting down...'


# def stop_decision():
#     global flag
#     flag = 1


# @ app.route('/stop', methods=['POST'])
# def stop():
#     stop_decision()
#     return '----------stop decision----------'


if __name__ == '__main__':
    # app.debug = True
    app.run(host='140.116.247.69', port=9000)
    agent.saved_model("0504.ckpt")
    if begin_plot == 1:
        agent.plot_loss()
