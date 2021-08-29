# Import necessary libraries
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation
import gym
from tqdm import tqdm
import time
from gym import spaces
from gym.utils import seeding


class Env(gym.Env):

    metadata = {'render.modes': ['human']}

    # 8/26 modifiy ,reward is -(delay)
    # stddev is not used
    def __init__(self, mean):
        assert len(mean.shape) == 2

        super(Env, self).__init__()
        # Define action and observation space
        self.protocols = mean.shape[1]
        self.num_experiments = mean.shape[0]
        self.action_space = spaces.Discrete(self.protocols)

        # Theres one state only in this problem
        self.observation_space = spaces.Discrete(1)
        self.mean = mean

    # used to return reward
    # 8/26 modifiy
    def step(self, action, imm_reward):
        # Sample from the specified bandit using it's reward distribution
        #assert (action < self.protocols).all()

        #sampled_means = self.mean[np.arange(self.num_experiments), action]

        # 8/26 modifiy ,reward is -1*delay
        reward = imm_reward

        # Return a constant state of 0. Our environment has no terminal state
        observation, done, info = 0, False, dict()
        return observation, reward, done, info

    def reset(self):
        return 0

    def render(self, mode='human', close=False):
        pass

    def _seed(self, seed=None):
        self.np_random, seed = seeding.np.random(seed)
        return [seed]

    def close(self):
        pass


class TestEnv(Env):
    def __init__(self, num_experiments=10, num_protocols=4):
        self.means = np.random.normal(size=(num_experiments, num_protocols))

        ArmedBanditsEnv.__init__(self, self.means, np.ones(
            (num_experiments, num_protocols)))
