import threading
cnt = 0


def printit():
    global cnt
    t = threading.Timer(5, printit)
    t.start()
    if cnt == 3:
        t.cancel()
    cnt += 1
    print("Hello, World!")


printit()
