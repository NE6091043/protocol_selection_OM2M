from flask import Flask, request

app = Flask(__name__)


@app.route('/delay', methods=['POST'])
def return_delay():
    print('..', request.data.decode())
    # if request == 'POST':
    #     if request.data < 1000:
    #         return 1
    #     else:
    #         return 2
    # else:
    #     return 100
    return "200"


@app.route('/action', methods=['POST'])
def do_action():
    print('..', request.data.decode())
    if request == 'POST':
        if request.data < 1000:
            return "coap"
        else:
            return "xmpp"
    else:
        return "websocket"
    return "mqtt"


if __name__ == '__main__':
    #app.debug = True
    app.run(host='140.116.247.69', port=9000)
