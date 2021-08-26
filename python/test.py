# Import necessary libraries
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation
import gym
from tqdm import tqdm
import time
from gym import spaces
from gym.utils import seeding
from IPython.display import HTML
from http.server import SimpleHTTPRequestHandler, HTTPServer
from flask import Flask, render_template
import flask_get
