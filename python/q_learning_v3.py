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


app = Flask(__name__)

m = dict()
m["{0, 100, 0}"] = 283.992
m["{0, 500, 0}"] = 683.752
m["{0, 1000, 0}"] = 1183.992
m["{0, 1500, 0}"] = 2066.76

m["{0, 100, 10}"] = 340.624
m["{0, 500, 10}"] = 856.192
m["{0, 1000, 10}"] = 1433.61
m["{0, 1500, 10}"] = 2537.524

m["{0, 100, 20}"] = 422.704
m["{0, 500, 20}"] = 1068.574
m["{0, 1000, 20}"] = 1784.252
m["{0, 1500, 20}"] = 3058.824

m["{0, 100, 30}"] = 514.3333333
m["{0, 500, 30}"] = 1352.71
m["{0, 1000, 30}"] = 2271.22
m["{0, 1500, 30}"] = 3848.254

# mqtt
m["{1, 100, 0}"] = 353.604
m["{1, 500, 0}"] = 753.34
m["{1, 1000, 0}"] = 1253.868
m["{1, 1500, 0}"] = 1821.984

m["{1, 100, 10}"] = 413.92
m["{1, 500, 10}"] = 872.764
m["{1, 1000, 10}"] = 1428.636
m["{1, 1500, 10}"] = 2044.452

m["{1, 100, 20}"] = 498.216
m["{1, 500, 20}"] = 1003.728
m["{1, 1000, 20}"] = 1772.892
m["{1, 1500, 20}"] = 2271.204

m["{1, 100, 30}"] = 569.864
m["{1, 500, 30}"] = 1242.908
m["{1, 1000, 30}"] = 2171.644
m["{1, 1500, 30}"] = 2690.66

# xmpp
m["{2, 100, 0}"] = 492.342
m["{2, 500, 0}"] = 890.242
m["{2, 1000, 0}"] = 1384.51
m["{2, 1500, 0}"] = 1912.656

m["{2, 100, 10}"] = 609.014
m["{2, 500, 10}"] = 1084.934
m["{2, 1000, 10}"] = 1761.67
m["{2, 1500, 10}"] = 2248.01

m["{2, 100, 20}"] = 746.22
m["{2, 500, 20}"] = 1337.744
m["{2, 1000, 20}"] = 1960.796
m["{2, 1500, 20}"] = 2663.53

m["{2, 100, 30}"] = 1002.618
m["{2, 500, 30}"] = 1709.032
m["{2, 1000, 30}"] = 2603.74
m["{2, 1500, 30}"] = 2801.478

# ws
m["{3, 100, 0}"] = 306
m["{3, 500, 0}"] = 706
m["{3, 1000, 0}"] = 1206
m["{3, 1500, 0}"] = 1722.704

m["{3, 100, 10}"] = 369.96
m["{3, 500, 10}"] = 901.42
m["{3, 1000, 10}"] = 1470.432
m["{3, 1500, 10}"] = 2016.668

m["{3, 100, 20}"] = 463.092
m["{3, 500, 20}"] = 1075.612
m["{3, 1000, 20}"] = 1818.004
m["{3, 1500, 20}"] = 2341.494

m["{3, 100, 30}"] = 472.954
m["{3, 500, 30}"] = 1250.876
m["{3, 1000, 30}"] = 2096.568
m["{3, 1500, 30}"] = 2639.49

# Initialize the environment of our problem
# num_protocols = 4
#protocols = ['COAP', 'MQTT', 'WebSocket', 'XMPP']

# average delay(ms) of data bytes between 100、500、1000、1500 event-driven in packet loss rate 0 %  means
# coap:26.705  mqtt:82.5635  ws:105.82  xmpp:96.9525

# get data from gscl and return action based on test_update


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
            print(self.q_table)
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


agent = QLearningTable(actions=list(range(4)))

# store 1000 data avoid buffer too large


def def_value():
    return "none"


record = defaultdict(def_value)

# state and its chosen action
sadict = dict()


def change_state_range(size):
    # base 1500
    # max  3500
    return 1+(size-1500)//100


# define init state
# state = [data size , avg loss rate before this time]
# idx for the state order
#action = np.int64(0)
# s=[]
# immreward=0

path = 'result.txt'
f = open(path, 'w')


@app.route('/get_data_size_return_action', methods=['POST'])
def get_data_size_return_action():

    # data[0]=datasize,data[1]=idx  for next state
    data = request.data.decode().split('//')
    datasize = float(data[0])
    idx = int(data[1])
    idx %= 50
    # global action,s,immreward
    # learn agent from state before
    # if idx!=0 and idx+1==float(data[1]):
    #     s_=[change_state_range(float(data[0])),0.0]
    #     agent.learn(str(s),action,immreward,str(s_))

    # state = [data size , avg loss rate before this time]
    # s=[change_state_range(size),0.0]

    # action = agent.choose_action(str(s))
    action = agent.choose_action(str(record[idx % 500]))
    agent.action = action
    sadict[str(record[idx % 500])] = action
    # print('..', size)
    # print("ok")
    # print('--------------***********************------------------')
    # print(action)
    # print('--------------***********************------------------')

    if action == 0:
        print("coap")
        f.write("0")
        f.write("\n")
        return "coap"
    if action == 1:
        print("xmpp")
        f.write("2")
        f.write("\n")
        return "xmpp"
    if action == 2:
        print("mqtt")
        f.write("1")
        f.write("\n")
        return "mqtt"
    if action == 3:
        print("ws")
        f.write("3")
        f.write("\n")
        return "ws"

    return ""

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
    idx %= 50
    est_loss = float(data[2])
    size = int(data[3])

    s_ = s = record[idx % 500]
    a = sadict[str(s)]

    tmp = size
    size = tmp-(size % 100)

    if est_loss < 10:
        if est_loss > 5:
            est_loss = 10
        else:
            est_loss = 0
    elif est_loss < 20:
        if est_loss > 15:
            est_loss = 20
        else:
            est_loss = 10
    elif est_loss < 30:
        if est_loss > 25:
            est_loss = 30
        else:
            est_loss = 20
    else:
        est_loss = 30

    condition = "{"+str(a)+", "+str(size)+", "+str(est_loss)+"}"

    r = (-0.002)*(delay)+size/m[condition]
    if (idx-1 >= 0 and ((idx-1) % 500) in record):
        # get state by idx
        s_ = record[(idx-1) % 500]
    agent.learn(str(s), a, r, str(s_))
    s = s_
    # agent.learn(str(s), a, r, str(s_))

    # res = data.split('//')
    # res[0]=delay
    # res[1]=protocol.

    # print(res[0])
    # print(res[1])
    # imm_reward = res[0]/res[1]
    return ""


def shutdown_server():
    func = request.environ.get('werkzeug.server.shutdown')
    if func is None:
        raise RuntimeError('Not running with the Werkzeug Server')
    func()


@ app.route('/shutdown', methods=['POST'])
def shutdown():
    shutdown_server()
    return 'Server shutting down...'


if __name__ == '__main__':
    # app.debug = True
    app.run(host='140.116.247.69', port=9000)
