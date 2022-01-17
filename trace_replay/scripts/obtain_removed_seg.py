import sys
from parse import parse

files = sys.argv[1:]
mp = {}

for filename in files:
  f = open(filename, "r")
  for line in f.readlines():
    if line.startswith("segment"):
      logid = parse('segments/{}/{}', line)[0]
      valid = True
    if line.startswith("rmed_seg") and valid:
      valid = False
      if logid not in mp:
        mp[logid] = []
        gp = parse('rmed_seg: {} {} {} {} {} {} {}', line)[0]

        mp[logid].append(gp)

for logid in mp:
  print(logid, mp[logid])
