    x = random.randint(0, 3)
    if x == 0:
        print("coap")
        return "coap"
    if x == 1:
        print("mqtt")
        return "mqtt"
    if x == 2:
        print("ws")
        return "ws"
    if x == 3:
        print("xmpp")
        return "xmpp"