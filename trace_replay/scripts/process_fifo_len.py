# (Exp#7) Memory Overhead: extract the number of unique LBAs in the FIFO queue
import sys

fifo_size_per_log = {}
num_lbas_per_log = {}
final_fifo_size_per_log = {}
final_num_lbas_per_log = {}

f = open(sys.argv[1], "r")
for line in f.readlines():
    words = line.rstrip().replace(" ", "").split(",")
    logid = words[0]
    fifo_size = words[1]
    num_lbas = words[2]

    key, value = fifo_size.split(":")
    if key == "currentFIFOSize":
        if logid not in fifo_size_per_log:
            fifo_size_per_log[logid] = []
        fifo_size_per_log[logid].append(value)
    elif key == "finalFIFOsize":
        if logid not in final_fifo_size_per_log:
            final_fifo_size_per_log[logid] = value

    key, value = num_lbas.split(":")
    if key == "numLBA":
        if logid not in num_lbas_per_log:
            num_lbas_per_log[logid] = []
        num_lbas_per_log[logid].append(value)
    elif key == "finalnumberofLBAs":
        if logid not in final_num_lbas_per_log:
            final_num_lbas_per_log[logid] = value


print("volumeid", "avg_num_lbas", "max_num_lbas", "min_num_lbas", "final_num_lbas")
for logid in final_num_lbas_per_log:
    if logid not in num_lbas_per_log:
        print(logid, final_num_lbas_per_log[logid], final_num_lbas_per_log[logid], final_num_lbas_per_log[logid], final_num_lbas_per_log[logid])
        continue

    lbas = num_lbas_per_log[logid]
    lbas = lbas[int(len(lbas) * 0.1):]
    tot = 0
    mx = 0
    mi = sys.maxsize
    for lba in lbas:
        n = int(lba)
        tot += n
        mx = max(mx, n)
        mi = min(mi, n)
    print(logid, tot / len(lbas), mx, mi, final_num_lbas_per_log[logid])
