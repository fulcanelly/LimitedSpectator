require 'socket'
require 'yaml'
require 'ostruct'
require 'json'
serv = TCPServer.new(2022)

users = {}

def map_val(x, i_min, i_max, o_min, o_max)
    inp = i_max - i_min
    out = o_max - o_min
    k = out / inp 
    return k * (I_min + x)
end

class Stats 
    def initialize()
        @arr = []
    end

    def in_same_range?(a, b) 
        error = 0.0001
        (a - error < b) and (a + error > b)
    end

    def add(x, y)
        entry = @arr.find do 
            in_same_range?(_1.x, x) and in_same_range?(_1.y, y)
        end

        if entry then 
            entry.count += 1

            entry.x = (entry.x + x) / 2 
            entry.y = (entry.y + y) / 2 

            return
        end

        @arr << OpenStruct.new({
            x: x, y: y, count: 1
        })
    end

    def to_s
        JSON.pretty_generate(
            @arr.sort_by do -_1.count end
                .map do _1.to_h end
        )
    end


end

loop do
    sock = serv.accept 
    res = OpenStruct.new(YAML.load(sock.gets))
    pp res
    users[res.name] ||= Stats.new
    users[res.name].add(res.x, res.y)

    File.write('res.txt',  users[res.name].to_s)
end
