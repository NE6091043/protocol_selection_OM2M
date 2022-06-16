# from os import pread
from flask import Flask, request
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation
from gym.utils import seeding
import pandas as pd
from collections import defaultdict
import time
import threading
import requests


app = Flask(__name__)
flag = 0


# second field is to identify if message get
this_time_reward = [0.0, True]
prev_time_reward = [0.0, False]
# [0,1,2,3]=[mqtt,coap,ws,xmpp]
prev_time_protocol = 0
env_loss, env_bandwidth = 0.0, 0
datasize = 0
diff = 0.0
min_D, max_D = 0, 100
min_val_delay, max_val_delay = 0.00002, 1.0
min_val_eff, max_val_eff = 0.0, 1.0

alpha, beta = 0.5, 0.5


class QLearningTable:
    def __init__(self, actions, learning_rate=0.01, reward_decay=0.9, e_greedy=0.9):
        self.actions = actions  # a list
        self.lr = learning_rate
        self.gamma = reward_decay
        self.epsilon = e_greedy
        self.q_table = pd.DataFrame(columns=self.actions, dtype=np.float64)
        self.decay = 0.999
        self.min_egreedy = 0.05

    def choose_action(self, observation):
        self.check_state_exist(observation)
        # action selection
        if np.random.uniform() > self.epsilon:
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
        # print('8888888888888888888888888888888888')
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


mncse = 0


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


def changeprotocol(protocol):
    global mncse
    global datasize, env_loss, env_bandwidth
    if mncse == 1:
        r = requests.post('http://140.116.247.69:18787/mncse', protocol)
        # a=datasize used
        datasize, env_loss, env_bandwidth = r.text.split('//')
    else:
        r = requests.post('http://140.116.247.69:18686/incse', protocol)
        # a=datasize used
        datasize, env_loss, env_bandwidth = r.text.split('//')


def start_decision():

    time.sleep(3)

    # -----------------decision part-----------------
    def decision():
        global flag
        global this_time_reward, prev_time_protocol
        global datasize, env_loss, env_bandwidth
        # decision per 1 seconds
        t = threading.Timer(3, decision)
        if flag == 1:
            t.cancel()
        t.start()

        agent.epsilon = max(agent.epsilon*agent.decay, agent.min_egreedy)
        reward = this_time_reward[0]

        # count diff and reset for next time used
        if this_time_reward[1] == False:
            # reward not update => delay so large
            # use prev reward => Give it a large penalty
            reward = -1000
        # print("------------------")
        # print("------------------")
        # print("------------------")
        # print(agent.epsilon)
        # print("------------------")
        # print("------------------")
        # print("------------------")
        # prev_time_reward = this_time_reward
        s = str([prev_time_protocol, datasize, env_loss, env_bandwidth])
        a = agent.choose_action(s)
        # [0,1,2,3]=[mqtt,coap,ws,xmpp]
        # post to OM2M
        # fetch next state from om2m
        # if prev_time_protocol == a:
        #     changeprotocol("unchanged")
        # if a == 0:
        #     changeprotocol("mqtt")
        # elif a == 1:
        #     changeprotocol("coap")
        # elif a == 2:
        #     changeprotocol("ws")
        # else:
        #     changeprotocol("xmpp")
        changeprotocol("ws")
        s_ = str([a, datasize, env_loss, env_bandwidth])
        agent.learn(s, a, reward, s_)
        prev_time_protocol = a
        # set as false
        this_time_reward[1] = False
    decision()


@ app.route('/receive_reward', methods=['POST'])
def receive_reward():
    global this_time_reward, alpha, beta
    delay, effi = request.data.decode().split('//')
    r = alpha*normalize(float(delay), 1)+beta*normalize(float(effi), 0)
    # print("------------------")
    # print("------------------")
    # print("------------------")
    # print(1/float(delay))
    # print(float(effi))
    # print(normalize(float(delay), 1))
    # print(normalize(float(effi), 0))
    # print(r)
    # print("------------------")
    # print("------------------")
    # print("------------------")
    this_time_reward[0] = r
    this_time_reward[1] = True
    # print("------------------")
    # print("------------------")
    # print("------------------")
    # print(prev_time_reward[0])
    # print(this_time_reward[0])
    # print("------------------")
    # print("------------------")
    # print("------------------")
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


def stop_decision():
    global flag
    flag = 1


@ app.route('/stop', methods=['POST'])
def stop():
    stop_decision()
    return '----------stop decision----------'


@ app.route('/start', methods=['POST'])
def start():
    global flag, diff
    flag = 0
    diff = 0.0
    start_decision()
    return 'start decision'


if __name__ == '__main__':
    app.run(host='140.116.247.69', port=9000)
