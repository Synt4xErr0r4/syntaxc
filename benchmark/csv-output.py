#!/usr/bin/python3
import re
import sys

benchmarks = [ 'Matrix', 'CRC', 'AES', 'Quicksort' ]

if len(sys.argv) != 4:
    print('Syntax: ' + sys.argv[0] + ' <NAME> <INPUT> <OUTPUT>')
    sys.exit(1)

REGEX_STRING='.+\n.+\nMin.+?(\d+) µs\nMax.+?(\d+) µs\nAvg.+?(\d+(?:.\d+)?) µs'

with open(sys.argv[2], 'r') as fi:
    with open(sys.argv[3], 'r') as fo:
        res_lines = fo.readlines()

    with open(sys.argv[3], 'w') as fo:
        compiler_name = sys.argv[1]
        data_lines = ''.join(fi.readlines())
        
        for benchmark in benchmarks:
            match = re.findall(benchmark + REGEX_STRING, data_lines)

            data = ( '0', '0', '0' )

            if len(match) == 0:
                print(f'Missing data for benchmark {benchmark} ({compiler_name}')
            else:
                data = (
                    match[0][0],
                    match[0][1],
                    match[0][2]
                )

            found = False
            n = len(res_lines)

            for i in range(n):
                line = res_lines[i]
                if benchmark in line:
                    idx = n

                    for j in range(i + 1, n):
                        if res_lines[j] == '\n':
                            idx = j
                            break
                    
                    xmin = data[0]
                    xmax = data[1]
                    xavg = data[2].replace('.', ',')

                    res_lines.insert(idx, f'"{compiler_name}";{xmin};{xavg};{xmax}\n')
                    found = True
                    break
            
            if not found:
                print(f'Could not find insertion point for benchmark data ({benchmark}, {compiler_name})')
            
        fo.writelines(res_lines)
