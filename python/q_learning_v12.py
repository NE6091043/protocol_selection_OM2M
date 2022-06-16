from pickle import FALSE
from click import BadArgumentUsage
from flask import Flask, request
import numpy as np
import random
import pandas as pd
import os
import xlsxwriter


min_D, max_D = 0, 100
min_val_delay, max_val_delay = 0.001, 1.0
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
agent.load_qtable("0518_zzz")
workbook = xlsxwriter.Workbook('result.xlsx')
result = workbook.add_worksheet()
result.write(0, 0, "result")
result.write(0, 1, "efficiency")

delay1, delay2 = 1, 1
st_0 = [-1, -1, -1, -1]
st_1 = [-1, -1, -1, -1]
# at_0 = np.int64(1)
prevdatasize, prevloss, prevbandwdth = 1000, 0, 500
flag = False


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


r1, r2 = 0, 0
datasize = 0
row = 1
at_1 = 1
reward = 1.0
qqq = 0


@app.route('/state_post_get_efficency', methods=['POST'])
def state_post_get_efficency():
    global row
    global st_0, st_1, at_1
    global delay1, delay2, flag, reward
    global r1, r2
    global datasize
    global qqq
    qqq += 1
    print("------------------------------------------------------")
    print("------------------------------------------------------")
    print("------------------------------------------------------")
    print("------------------------------------------------------")
    print("------------------------------------------------------")
    print(qqq)
    print("------------------------------------------------------")
    print("------------------------------------------------------")
    print("------------------------------------------------------")
    print("------------------------------------------------------")
    print("------------------------------------------------------")

    if flag == False:
        delay2 = 3000

    # State s
    st_0 = st_1
    data = request.data.decode().split('//')
    size2 = int(data[0])
    loss_rate = int(data[1])
    bandwidth = int(data[2])
    throughput = int(data[3])
    # st_1[1] = size2
    # st_1[2] = loss_rate
    # st_1[3] = bandwidth
    # st_1 = []
    st_1 = [at_1, size2, loss_rate, bandwidth]
    # print(size2)
    # print(loss_rate)
    # print(bandwidth)
    # print(throughput)
    if throughput == 0:
        throughput = datasize
    efficiency = 0
    if qqq >= 3:
        efficiency = datasize/throughput
        result.write(row, 0, row)
        result.write(row, 1, efficiency)
        row += 1
        if flag == False:
            reward = -1000
        else:
            x = alpha*normalize(float(delay), 1)
            y = beta*normalize(float(efficiency), 0)
            print(x)
            print(y)
        agent.learn(str(st_0), at_1, reward, str(st_1))
    # print("------------------------------------------------------")
    # print("------------------------------------------------------")
    # print("------------------------------------------------------")
    # print("------------------------------------------------------")
    # print("------------------------------------------------------")
    # print("------------------------------------------------------")
    # print(throughput)
    # if(efficiency > 1):
    #     efficiency = 0.8
    # print(efficiency)
    # print("------------------------------------------------------")
    # print("------------------------------------------------------")
    # print("------------------------------------------------------")
    # print("------------------------------------------------------")
    # print("------------------------------------------------------")
    # print("------------------------------------------------------")
    at_1 = agent.choose_action(str(st_1))
    datasize = size2
    flag = False
    # st_1[0] = at_1 = agent.choose_action(str(st_1))

    if qqq == 2:

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
    return ""

# update reward
# not returning anything


@app.route('/receive_delay', methods=['POST'])
def receive_delay():
    global delay1, delay2, flag
    global alpha, beta
    delay2 = int(request.data.decode())
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


@ app.route('/endserver', methods=['POST'])
def endserver():
    global row, result, datasize, workbook
    x = request.data.decode()
    throughput = int(x)
    result.write(row, 0, row)
    result.write(row, 1, datasize/throughput)
    workbook.close()

    print(datasize/throughput)

    shutdown_server()
    return 'Server shutting down...'


if __name__ == '__main__':
    # app.debug = True
    app.run(host='140.116.247.69', port=9000)
    agent.save_qtable("0518_zzz")
