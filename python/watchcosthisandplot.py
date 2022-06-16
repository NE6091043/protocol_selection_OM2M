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

filename = "test0610event"


def loadcost_his(filename):
    exist = os.path.exists(filename)
    if exist:
        with open(filename, "rb") as fp:
            b = pickle.load(fp)
        return b
    return []


costhis = loadcost_his("cost_hist/"+filename+"querycosthis")


def plot_loss(costhis):
    plt.plot(np.arange(len(costhis)), costhis)
    plt.ylabel('Cost')
    plt.xlabel('training steps')
    plt.show()


plot_loss(costhis)
