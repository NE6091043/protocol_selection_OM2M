import pandas as pd
import os
import numpy as np


def print_action(filename, s):
    exist = os.path.exists(filename)
    if not exist:
        return
    q_table = pd.read_pickle(filename)
    # more options can be specified also
    # x = q_table.sort_values(by=(0), ascending=True)
    # with pd.option_context('display.max_rows', None, 'display.max_columns', None):
    #     print(x)
    state_action = q_table.loc[s, :]
    print(np.random.choice(
        state_action[state_action == np.max(state_action)].index))


def load_qtable(filename):
    exist = os.path.exists(filename)
    if not exist:
        return
    q_table = pd.read_pickle(filename)
    # more options can be specified also
    x = q_table.sort_values(by=(1), ascending=True)
    with pd.option_context('display.max_rows', None, 'display.max_columns', None):
        print(x)


load_qtable("0523_event")

# coap:70.4e
# mqtt:75.57
# ws: 62.298
# xmpp: 66.910
