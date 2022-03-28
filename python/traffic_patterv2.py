# -*- coding: utf-8 -*-
"""
Created on Wed Aug 25 17:52:29 2021

@author: ne6091043
"""

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


app = Flask(__name__)

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
        # record for learning
        self.protocol="mqtt"

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
            action = np.random.choice(state_action[state_action == np.max(state_action)].index)
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

# number of protocols
num_protocols=1
agent=QLearningTable(actions=list(range(num_protocols)))

# store 1000 data avoid buffer too large
def def_value():
    return "none"
record=defaultdict(def_value)

# state and its chosen action
sadict=dict()

# def change_state_range(size):
#     # base 1500
#     # max  3500
#     return 1+(size-1500)//100
        

# define init state
# state = [data size , avg loss rate before this time]
# idx for the state order
#action = np.int64(0)
#s=[]
#immreward=0

@app.route('/get_data_size_return_action', methods=['POST'])
def get_data_size_return_action():
    
    # data[0]=datasize,data[1]=idx  for next state
    data = request.data.decode().split('//')
    datasize=int(data[0])
    idx=int(data[1])
    # fill in record
    # 5000 idx divide into 100 groups  ==> 5000/100 50個一組
    # 3000 is enough or it might loss
    # now protocol
    record[idx%3000]=[agent.protocol,math.ceil(idx/50),datasize]
    # global action,s,immreward
    # learn agent from state before
    # if idx!=0 and idx+1==float(data[1]):
    #     s_=[change_state_range(float(data[0])),0.0]
    #     agent.learn(str(s),action,immreward,str(s_))
    
       
    
    # state = [data size , avg loss rate before this time]
    # s=[change_state_range(size),0.0]
    
    # action = agent.choose_action(str(s))
    action = agent.choose_action(str(record[idx%3000]))
    sadict[str(record[idx%3000])]=action
    # print('..', size)
    # print("ok")
    # print('--------------***********************------------------')
    # print(action)
    # print('--------------***********************------------------')
    
    # same used no post
    if idx>1 and record[(idx-1)%3000]==record[(idx)%3000]:
        return " "
        
    if action.item() == 0:
        agent.protocol="xmpp"
        print("xmpp")
        x=requests.post('http://140.116.247.69:18787/test',"xmpp")
    elif action.item() == 1:
        agent.protocol="mqtt"
        print("mqtt")
        x=requests.post('http://140.116.247.69:18787/test',"mqtt")
    elif action.item() == 2:
        agent.protocol="ws"
        print("ws")
        x=requests.post('http://140.116.247.69:18787/test',"ws")
    else:
        # action.item() == 3:
        agent.protocol="coap"
        print("coap")
        x=requests.post('http://140.116.247.69:18787/test',"coap")
    return ""

# update reward 
# not returning anything


@app.route('/receive_delay_as_reward', methods=['POST'])
def receive_action_and_delay_as_reward():
    # global idx,immreward
    # state s_ is new observation
    # set immediate reward as delay/data_size
    data = request.data.decode().split('//')
    delay=float(data[0])
    idx=int(data[1])
    number=int(data[2])
    order=int(data[3])
    
    # order比number越大越好
    
    
    s=s_=record[idx%3000]
    a=sadict[str(s)]
    r=(order-number)-delay
    if record[(1+idx)%3000]!="none":
        #get state by idx
        s_=record[(1+idx)%3000]
    agent.learn(str(s), a, r, str(s_))
        
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