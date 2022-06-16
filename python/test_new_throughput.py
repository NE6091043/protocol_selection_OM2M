from re import S
from flask import Flask, request
# 用不到gym環境，環境為OM2M
# from agent import Env, TestEnv
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation
import gym
from tqdm import tqdm
import time
from gym import spaces
from gym.utils import seeding
import random
import pandas as pd
from collections import defaultdict
import os


min_D, max_D = 0, 100
min_val_delay, max_val_delay = 0.00002, 1.0
min_val_eff, max_val_eff = 0.0, 1.0

alpha, beta = 0.5, 0.5

app = Flask(__name__)


class QLearningTable:
    def __init__(self, actions, learning_rate=0.01, reward_decay=0.9, e_greedy=0.9):
        self.actions = actions  # a list
        self.lr = learning_rate
        self.gamma = reward_decay
        self.epsilon = e_greedy
        self.q_table = pd.DataFrame(columns=self.actions, dtype=np.float64)

    def choose_action(self, observation):
        self.check_state_exist(observation)
        # action selection
        if np.random.uniform() < self.epsilon:
            # choose best action
            # print(['COAP', 'MQTT', 'WebSocket', 'XMPP'])
            # print(self.q_table)
            state_action = self.q_table.loc[observation, :]
            # print(state_action)
            # some actions may have the same value, randomly choose on in these actions
            # print(state_action[state_action == np.max(state_action)])
            # print(state_action[state_action == np.max(state_action)].index)
            action = np.random.choice(
                state_action[state_action == np.max(state_action)].index)
        else:
            # choose random action
            action = np.random.choice(self.actions)
        # print(action)
        print('8888888888888888888888888888888888')
        return action

    def learn(self, s, a, r, s_):
        self.check_state_exist(s_)
        q_predict = self.q_table.loc[s, a]
        q_target = r + self.gamma * self.q_table.loc[s_, :].max()
        self.q_table.loc[s, a] += self.lr * (q_target - q_predict)  # update

    def check_state_exist(self, state):
        if state not in self.q_table.index:
            # append new state to q table
            self.q_table = self.q_table.append(
                pd.Series(
                    [0]*len(self.actions),
                    index=self.q_table.columns,
                    name=state,
                )
            )

    def save_qtable(self, filename):
        self.q_table.to_pickle(filename)

    def load_qtable(self, filename):
        exist = os.path.exists(filename)
        if not exist:
            return
        self.q_table = pd.read_pickle(filename)


agent = QLearningTable(actions=list(range(4)))
agent.load_qtable("0506_query")

reward = 1.0
st_0 = [0, 0, 0]
st_1 = [1000, 0, 500]
at_0 = np.int64(1)
prevdatasize, prevloss, prevbandwdth = 1000, 0, 500
flag = False
datasize = 0


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


@app.route('/state_post', methods=['POST'])
def state_post():
    global st_0, st_1, at_0
    global reward, flag
    global datasize
    # State s
    data = request.data.decode().split('//')
    datasize = int(data[0])
    loss_rate = int(data[1])
    bandwidth = int(data[2])
    # print(datasize)
    st_1 = [datasize, loss_rate, bandwidth]
    if flag == False:
        reward = -1000
    if st_0 != [0, 0.0, 0]:
        agent.learn(str(st_0), at_0, reward, str(st_1))
    flag = False
    at_0 = agent.choose_action(str(st_1))
    st_0 = st_1
    # if at_0 == 0:
    #     print("coap")
    #     return "coap"
    # if at_0 == 1:
    #     print("mqtt")
    #     return "mqtt"
    # if at_0 == 2:
    #     print("ws")
    #     return "ws"
    # if at_0 == 3:
    #     print("xmpp")
    #     return "xmpp"
    return "mqtt"

# update reward
# not returning anything


throughput = 0


@app.route('/receive_reward', methods=['POST'])
def receive_reward():
    global reward, flag
    global alpha, beta
    global datasize, throughput
    delay, _ = request.data.decode().split('//')
    # print(datasize)
    print(throughput)
    # effi = datasize/throughput
    # print(effi)
    reward = alpha*normalize(float(delay), 1)+beta*normalize(float(0.0), 0)
    flag = True

    return ""


@app.route('/receive_throughput', methods=['POST'])
def receive_throughput():
    global throughput
    throughput += int(request.data.decode())
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


if __name__ == '__main__':
    # app.debug = True
    app.run(host='140.116.247.69', port=9000)
    # agent.save_qtable("0506_query")
