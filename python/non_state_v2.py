from flask import Flask, request
from agent import Env, TestEnv
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation
import gym
from tqdm import tqdm
import time
from gym import spaces
from gym.utils import seeding

app = Flask(__name__)


# 先接收delay選動作
delay = 100
size = 0
action = 3

# Initialize the environment of our problem
num_experiments = 2
num_protocols = 4
num_steps = 100
means = np.random.normal(size=(num_experiments, num_protocols))



def argmax(q_values):

    # Generate a mask of the max values for each row
    mask = q_values == q_values.max(axis=1)[:, None]
    # Generate noise to be added to the ties
    r_noise = 1e-6*np.random.random(q_values.shape)
    # Get the argmax of the noisy masked values
    return np.argmax(r_noise*mask, axis=1)


class GreedyAgent:
    def __init__(self, reward_estimates):

        assert len(reward_estimates.shape) == 2

        self.num_protocols = reward_estimates.shape[1]
        self.num_experiments = reward_estimates.shape[0]
        self.reward_estimates = reward_estimates.astype(np.float64)
        self.action_count = np.zeros(reward_estimates.shape)

    def get_action(self):
        # Our agent is greedy, so there's no need for exploration.
        # Our argmax will do just fine for this situation
        action = argmax(self.reward_estimates)

        # Add a 1 to each action selected in the action count
        self.action_count[np.arange(self.num_experiments), action] += 1

        return action

    def update_estimates(self, reward, action):

        n = self.action_count[np.arange(self.num_experiments), action]

        # Compute the difference between the received rewards vs the reward estimates
        error = reward - \
            self.reward_estimates[np.arange(self.num_experiments), action]

        # Update the reward difference incementally
        self.reward_estimates[np.arange(
            self.num_experiments), action] += (1/n)*error


env = Env(means)

# Initialize the agent
agent = GreedyAgent(np.zeros((num_experiments, num_protocols)))

# Code for plotting the interaction
fig, axs = plt.subplots(1, num_experiments, figsize=(10, 4))
x_pos = np.arange(num_protocols)


def choose_protocol():
    protocol = agent.get_action()
    return protocol


# get data from gscl and return action based on test_update
@app.route('/get_data_size_return_action', methods=['POST'])
def get_data_size_return_action():
    # update
    size = request.data.decode()
    print('..', size)
    print("ok")
    action = choose_protocol()
    print(action)
    if action == 0:
        print("coap")
        return "coap"
    if action == 1:
        print("mqtt")
        return "mqtt"
    if action == 2:
        print("ws")
        return "ws"
    if action == 3:
        print("xmpp")
        return "xmpp"

# update reward and draw

# not returning anything


@app.route('/receive_delay_as_reward', methods=['POST'])
def receive_action_and_delay_as_reward():
    # stateless method
    # set immediate reward as delay/data_size
    data = request.data.decode()
    res = data.split('//')
    # res[0]=delay
    # res[1]=protocol

    print(res[0])
    print(res[1])
    # imm_reward = delay/size
    # update_reward(imm_reward)
    return "aaaa"


protocols = ['COAP', 'MQTT', 'WebSocket', 'XMPP']
# test1
# average delay(ms) of data bytes between 100、500、1000、1500 event-driven in packet loss rate 0 %  means
# coap:26.705  mqtt:82.5635  ws:105.82  xmpp:96.9525


def init():
    for i in range(num_experiments):
        init_ax(i)


def init_ax(i):
    ax = axs[i]
    ax.clear()
    ax.set_ylim(-4, 4)
    ax.set_xticks(range(4))  # ticks placed on [0, 1, ..., 11]
    ax.set_xticklabels(protocols)
    ax.set_xlabel('Protocols', fontsize=14)
    ax.set_ylabel('Value', fontsize=14)
    ax.set_title(label='Estimated Values vs. Mean values', fontsize=15)
    ax.plot(x_pos, env.mean[i], marker='D', linestyle='',
            alpha=0.8, color='r', label='Mean Values')
    ax.axhline(0, color='black', lw=1)

# Implement a step, which involves the agent acting upon the
# environment and learning from the received reward.


def update_reward(imm_reward):
    _, reward, _, _ = env.step(action, imm_reward)
    # update reward by delay
    # 8/26 modifiy
    agent.update_estimates(reward, action)


def plot_fig():
    for i in range(num_experiments):
        ax = axs[i]
        # Plot the estimated values from the agent compared to the real values
        estimates = agent.reward_estimates[i]
        init_ax(i)
        values = ax.bar(x_pos, estimates, align='center',
                        color='blue', alpha=0.4, label='Estimated Values')
        ax.legend()


# anim = FuncAnimation(fig, func=update_reward, frames=np.arange(
#     num_steps), init_func=plot_fig, interval=10, repeat=False, blit=False)
# plt.show()


if __name__ == '__main__':
    # app.debug = True
    app.run(host='140.116.247.69', port=9000)
