import time
from queue import Queue

class TimeManager:
    _instance = None
    def __init__(self):
        if not TimeManager._instance:
            self.elapsed_time = time.time()
            self.recorded_time = None
            self.events = {}
            self.event_result = {}

    @classmethod
    def get_instance(cls):
        if not cls._instance:
            cls._instance = TimeManager()
        return cls._instance
    
    def add_timer(self, id, after_time, *param):
        if id not in self.events.keys():
            self.events[id] = (self.elapsed_time + after_time, param)
            
    def remove_timer(self, id):
        if id in self.events.keys():
            del self.events[id]
        if id in self.event_result.keys():
            del self.event_result[id]
            
    def pick_result(self, id):
        if id in self.event_result.keys():
            return self.event_result[id]
        return None
        
    def update(self):
        self.elapsed_time = time.time()
        
        for key, value in self.events.items():
            if self.elapsed_time >= value[0]:
                if key not in self.event_result.keys():
                    self.event_result[key] = Queue()
                self.event_result[key].put(*value[1])
                
                
class Observer:
    _instance = None
    def __init__(self):
        if not Observer._instance:
            self.values = {}
            
    @classmethod
    def get_instance(cls):
        if not cls._instance:
            cls._instance = Observer()
        return cls._instance
    
    def on_change(self, id, value):
        if id in self.values.keys():
            if self.values[id] != value:
                result = (self.values[id], value)
                self.values[id] = value
                return result
                
        self.values[id] = value
        return self.values[id], value
