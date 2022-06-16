a = 1000
file = open('x.txt', 'w')
file.writelines(str(a))
file.close()

# file = open('x.txt', 'r')
# Lines = file.readlines()
# for line in Lines:
#     x = int(line)
#     print(x)
