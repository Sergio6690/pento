import multiprocessing

def copy_to_q(fn,q,args):
    q.put(fn(args))


class parallelize:

    def splits_to_core(self, arg_list):
        cores = multiprocessing.cpu_count()
        if cores > len(arg_list):
            return ([arg_list],cores)
        arglist_per_core = len(arg_list)/cores
        split_starts = range(0,len(arg_list),arglist_per_core)
        splits = map(lambda c : arg_list[c:c+arglist_per_core], split_starts)
        return (splits,cores)

    def do_parallel(self, arg_list):
        (splits,cores) = self.splits_to_core(arg_list)
        if len(splits) == 1:
            return self.f(splits[0])
        q = multiprocessing.Queue()
        processes = []
        for core in range(cores):
            p = multiprocessing.Process(target=copy_to_q,args=(self.f,q,splits[core]))
            # print `len(splits[core])` + " Starting For " +  str(splits[core])
            processes.append(p)
        for p in processes:
            p.start()
        results = []
        for i in range(cores):
            results.extend(q.get())
        results.extend(self.f(splits[-1]))
        print len(results)
        return results


    def __init__(self,f):
        self.f = f

    def __call__(self, *args, **kwargs):
        return self.do_parallel(*args)

