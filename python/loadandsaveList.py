import pickle
l = [1, 2, 3, 4]
with open("sss", "wb") as fp:
    pickle.dump(l, fp)
with open("sss", "rb") as fp:
    b = pickle.load(fp)
print(b)
