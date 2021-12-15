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
from gym import spaces
from gym.utils import seeding
import random
import pandas as pd
from collections import defaultdict
import requests


dictToSend = {'question':'what is the answer?'}

i=0
s="coap"

while i<50000:
    time.sleep(3)
    if i%4==0:
        s="coap"
    if i%4==1:
        s="mqtt"
    if i%4==2:
        s="ws"
    if i%4==3:
        s="xmpp"
    i+=1
    requests.post('http://140.116.247.69:14000/test',s)

