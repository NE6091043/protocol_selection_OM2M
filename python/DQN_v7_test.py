# -*- coding: utf-8 -*-
from difflib import restore
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
import pickle

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
            e_greedy=1.0,
            # 多少不步把eval network參數更新到target network
            replace_target_iter=200,
            memory_size=5000,
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
        # self.decay = 0.999
        # self.min_egreedy = 0.05

        # self.action = 1
        self.step = 0
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
        self.load_model("checkpoints/dqn0523event")

        if output_graph:
            # $ tensorboard --logdir=logs
            # tf.train.SummaryWriter soon be deprecated, use following
            # 輸出tensorboard
            tf.summary.FileWriter("logs/", self.sess.graph)

        # x = self.sess.run(tf.global_variables_initializer())
        # print("all values %s" % self.sess.run(
        #     tf.global_variables_initializer()))
        self.sess.run(tf.global_variables_initializer())
        # self.cost_his = self.loadcost_his("cost_hist/dqn0524querycosthis")

    def save_step(self, filename):
        file = open(filename, 'w')
        file.writelinesf
        (str(self.step))
        file.close()

    def load_step(self, filename):
        exist = os.path.exists(filename)
        if exist:
            file = open(filename, 'r')
            Lines = file.readlines()
            for line in Lines:
                x = int(line)
                return x
        return 0

    def savecost_his(self, filename):
        with open(filename, "wb") as fp:
            pickle.dump(self.cost_his, fp)

    def loadcost_his(self, filename):
        exist = os.path.exists(filename)
        if exist:
            with open(filename, "rb") as fp:
                b = pickle.load(fp)
            return b
        return []

    def _build_net(self):
        # ------------------ build evaluate_net ------------------
        self.s = tf.placeholder(
            tf.float32, [None, self.n_features], name='s')  # input
        self.q_target = tf.placeholder(
            tf.float32, [None, self.n_actions], name='Q_target')  # for calculating loss
        with tf.variable_scope('eval_net'):
            # c_names(collections_names) are the collections to store variables
            # 第一層輸出神經元10個
            c_names, n_l1, w_initializer, b_initializer = \
                ['eval_net_params', tf.GraphKeys.GLOBAL_VARIABLES], 32, \
                tf.random_normal_initializer(
                    0., 0.3), tf.constant_initializer(0.1)  # config of layers

            # first layer. collections is used later when assign to target net
            with tf.variable_scope('l1'):
                w1 = tf.get_variable(
                    'w1', [self.n_features, n_l1], initializer=w_initializer, collections=c_names)
                b1 = tf.get_variable(
                    'b1', [1, n_l1], initializer=b_initializer, collections=c_names)
                l1 = tf.nn.relu(tf.matmul(self.s, w1) + b1)

            with tf.variable_scope('l2'):
                w2 = tf.get_variable(
                    'w2', [n_l1, n_l1], initializer=w_initializer, collections=c_names)
                b2 = tf.get_variable(
                    'b2', [1, n_l1], initializer=b_initializer, collections=c_names)
                l2 = tf.nn.relu(tf.matmul(l1, w2) + b2)

            # second layer. collections is used later when assign to target net
            with tf.variable_scope('l3'):
                w3 = tf.get_variable(
                    'w3', [n_l1, self.n_actions], initializer=w_initializer, collections=c_names)
                b3 = tf.get_variable(
                    'b3', [1, self.n_actions], initializer=b_initializer, collections=c_names)
                self.q_eval = tf.matmul(l2, w3) + b3

        with tf.variable_scope('loss'):
            self.loss = tf.reduce_mean(
                tf.squared_difference(self.q_target, self.q_eval))
        with tf.variable_scope('train'):
            self._train_op = tf.train.AdamOptimizer(
                self.lr).minimize(self.loss)

        # ------------------ build target_net ------------------
        self.s_ = tf.placeholder(
            tf.float32, [None, self.n_features], name='s_')    # input
        with tf.variable_scope('target_net'):
            # c_names(collections_names) are the collections to store variables
            # 第二層輸出神經元n個actions
            c_names = ['target_net_params', tf.GraphKeys.GLOBAL_VARIABLES]

            # first layer. collections is used later when assign to target net
            with tf.variable_scope('l1'):
                w1 = tf.get_variable(
                    'w1', [self.n_features, n_l1], initializer=w_initializer, collections=c_names)
                b1 = tf.get_variable(
                    'b1', [1, n_l1], initializer=b_initializer, collections=c_names)
                l1 = tf.nn.relu(tf.matmul(self.s_, w1) + b1)

            with tf.variable_scope('l2'):
                w2 = tf.get_variable(
                    'w2', [n_l1, n_l1], initializer=w_initializer, collections=c_names)
                b2 = tf.get_variable(
                    'b2', [1, n_l1], initializer=b_initializer, collections=c_names)
                l2 = tf.nn.relu(tf.matmul(l1, w2) + b2)

            # second layer. collections is used later when assign to target net
            with tf.variable_scope('l3'):
                w3 = tf.get_variable(
                    'w3', [n_l1, self.n_actions], initializer=w_initializer, collections=c_names)
                b3 = tf.get_variable(
                    'b3', [1, self.n_actions], initializer=b_initializer, collections=c_names)
                self.q_next = tf.matmul(l2, w3) + b3

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
            # print('--------------***********************------------------')
            # print('--------------***********************------------------')
            # print('--------------***********************------------------')
            # print('--------------***********************------------------')
            # print('--------------***********************------------------')
            # print(actions_value)
            # print('--------------***********************------------------')
            # print('--------------***********************------------------')
            # print('--------------***********************------------------')
            # print('--------------***********************------------------')
            # print('--------------***********************------------------')
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
        # 隨機抽取記憶from batch memory
        # 如果記憶庫沒有那麼多資料就隨機抽
        if self.memory_counter > self.memory_size:
            sample_index = np.random.choice(
                self.memory_size, size=self.batch_size)
        else:
            sample_index = np.random.choice(
                self.memory_counter, size=self.batch_size)
        batch_memory = self.memory[sample_index, :]

        # q-target 跟 q-eval
        # 抽features裡面的頭 跟store時放的順序有關
        # 抽features裡面的尾 跟store時放的順序有關
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
        # self.cost_his.append(self.cost)

        # increasing epsilon
        # self.epsilon = self.epsilon + \
        #     self.epsilon_increment if self.epsilon < self.epsilon_max else self.epsilon_max
        self.learn_step_counter += 1

    def plot_loss(self):
        plt.plot(np.arange(len(self.cost_his)), self.cost_his)
        plt.ylabel('Cost')
        plt.xlabel('training steps')
        plt.show()

    def saved_model(self, filename):
        tf.train.Saver().save(self.sess, filename, write_meta_graph=False)

    def load_model(self, filename):
        exist = os.path.exists(filename)
        if not exist:
            return
        tf.train.Saver().restore(self.sess, filename)


# action,features
# 4,4(protocol,size,loss,bandwidth)
agent = DQN(4, 4, e_greedy=1.0, output_graph=True)
# agent.step = agent.load_step("cost_hist/dqn0524querystep")

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
            res = 100*(1000-val)/(1000-1)
    else:
        if max_val_eff == min_val_eff:
            res = 50
        else:
            res = 100*(val-min_val_eff)/(max_val_eff-min_val_eff)
    return round(res, 5)


# reward = 1.0
# st_0 = [1, 0, 0, 0]
# st_1 = [1, 1000, 0, 500]
# at_1 = 1
# prevdatasize, prevloss, prevbandwdth = 1000, 0, 500
# flag = False
reward_list = [[1, False] for _ in range(101)]
st_0 = [1, 1000, 0, 500]
st_1 = [1, 1000, 0, 500]
st_2 = [1, 1000, 0, 500]
at_0, at_1 = 1, 1
prevdatasize, prevloss, prevbandwdth = 1000, 0, 500


@app.route('/state_post', methods=['POST'])
def state_post():
    # global st_0, st_1, at_1
    # global reward, flag
    # data = request.data.decode().split('//')
    # datasize = int(data[0])
    # loss_rate = int(data[1])
    # bandwidth = int(data[2])
    # st_0[0] = at_1
    # st_1[1] = datasize
    # st_1[2] = loss_rate
    # st_1[3] = bandwidth
    # if flag == False:
    #     reward = -1000
    # st_1[0] = at_1 = agent.choose_action(np.array(st_1))
    # if agent.step != 0:
    #     agent.store_transition(np.array(st_0), st_0[0], reward, np.array(st_1))
    # agent.step += 1
    # if agent.step >= 50 and agent.step % 5 == 0:
    #     agent.learn()
    # flag = False
    # st_0[0] = datasize
    # st_0[1] = loss_rate
    # st_0[2] = bandwidth
    global st_0, st_1, st_2, at_0, at_1
    global reward_list
    reward, reward1, reward2 = 0, 0, 0
    data = request.data.decode().split('//')
    idx = int(data[0])
    datasize = int(data[1])
    loss_rate = int(data[2])
    bandwidth = int(data[3])
    st_0 = st_1
    st_1 = st_2
    st_2 = [at_1, datasize, loss_rate, bandwidth]

    if idx > 0:
        if reward_list[(idx-1) % 101][1] == False:
            reward2 = -1000
        else:
            reward2 = reward_list[(idx-1) % 101][0]
        reward_list[(idx-1) % 101][1] = False

    if idx > 1:
        reward1 = reward_list[(idx-2) % 101][0]
        reward_list[(idx-2) % 101][1] = False

    reward = reward2-reward1
    if idx >= 3:
        print("---------------------------------")
        print("---------------------------------")
        print("---------------------------------")
        print("---------------------------------")
        print(st_0)
        print(at_0)
        print(reward)
        print(st_1)
        print("---------------------------------")
        print("---------------------------------")
        print("---------------------------------")
        print("---------------------------------")
        agent.store_transition(np.array(st_0), at_0, reward, np.array(st_1))
        agent.step += 1
    if agent.step >= 50 and agent.step % 5 == 0:
        agent.learn()

    at_0 = at_1
    at_1 = agent.choose_action(np.array(st_2))

    if at_1 == 0:
        print("coap")
        return "coap"
    if at_1 == 1:
        print("mqtt")
        return "mqtt"
    if at_1 == 2:
        print("ws")
        return "ws"
    if at_1 == 3:
        print("xmpp")
        return "xmpp"
    return "mqtt"


@app.route('/receive_reward', methods=['POST'])
def receive_reward():
    global alpha, beta
    global reward_list
    order, delay, effi = request.data.decode().split('//')
    x = alpha*normalize(float(delay), 1)+beta*normalize(float(effi), 0)
    idx = int(order)
    reward_list[idx % 101][0] = x
    reward_list[idx % 101][1] = True
    return ""


# begin_plot = 0


def shutdown_server():
    func = request.environ.get('werkzeug.server.shutdown')
    if func is None:
        raise RuntimeError('Not running with the Werkzeug Server')
    func()


@ app.route('/shutdown', methods=['POST'])
def shutdown():
    # global begin_plot
    # plot = request.data.decode()
    # if plot == "1":
    #     begin_plot = 1
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
    # agent.saved_model("checkpoints/dqn0523query")
    # agent.savecost_his("cost_hist/dqn0524querycosthis")
    # agent.save_step("cost_hist/dqn0524querystep")
    # agent.plot_loss()
