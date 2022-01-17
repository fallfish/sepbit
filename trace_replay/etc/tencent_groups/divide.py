import sys

n_groups = sys.argv[1]

files = []
for i in range(1, int(n_groups) + 1):
    files.append(open("group" + str(i), "w"))

filelist = open("selected_volumes.txt", "r")

cnt = 0
for l in filelist.readlines():
    files[cnt % int(n_groups)].write(l)
    cnt += 1

for f in files:
    f.close()
