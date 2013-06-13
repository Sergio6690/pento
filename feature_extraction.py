import BaseHTTPServer
from SocketServer import ThreadingMixIn
import unittest
import urlparse
import soundex
from functools import partial
import re
import json
from math import log
from operator import add
import sys
import os

def read(fl):
    return map(lambda x: x.strip().lower(),open(fl).readlines())

apply_soundex = partial(soundex.soundex,len=5)

first_names_db  = read("top_first_names")
last_names_db = read("top_last_names")

first_names_soundex = map(apply_soundex,first_names_db)
last_names_soundex =  map(apply_soundex,last_names_db)

verbs = read('verbs')
nouns = read('nouns')
plurals = map(lambda x: x+'s' , nouns)
plurals = filter(lambda x: not x.endswith('ss'),plurals)
adjec = read('adjectives')
advbs = read('adverbs')
other = read('otherwords')
allwrds = set(verbs + nouns + adjec + advbs + other + plurals)
allwrds = filter(lambda x: len(x)>1, allwrds)
allwrds.append("a")
common_email_domains = set(read('common_email_domains'))
group_email_domains = set(read("group_email_domains"))
names = set(filter(lambda x: len(x)>2, first_names_db + last_names_db))

#Utils

def segment(combined_word, dictionary):
    possibilities = []
    if combined_word in dictionary:
        possibilities.append([combined_word])
    for i in range(len(combined_word)):
        if combined_word[0:i] in dictionary:
            rest = segment(combined_word[i:],dictionary)
            if len(rest) > 0:
                possibilities.append([combined_word[0:i]] + rest)
    possibilities.sort(cmp = lambda a,b :cmp(len(a),len(b)))
    if len(possibilities) > 0:
        return possibilities[0]
    return []

def split_camel_case(name):
    s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
    return  re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()

def is_in_index(indx, test_wrd, use_soundex = False):
    if use_soundex:
        return apply_soundex(test_wrd) in indx
    return test_wrd in indx

def clean_id(id):
    return id.split('+')[0]

def id_words(id):
    id = split_camel_case(id)
    if id.find('.') > 0:
        return id.split('.')
    if id.find('_') > 0:
        return id.split('_')
    if id.find('-') > 0:
        return id.split('-')
    wrdsplit = segment(id,allwrds)
    if len(wrdsplit)>0:
        return wrdsplit
    namesplit = segment(id,names)
    if len(namesplit) > 0:
        return namesplit
    return [id]

#FEATURES

def has_name(id, wrds, dom, tld, complete_address):
    return int(is_in_index(last_names_soundex,id[1:], True) or
    is_in_index(first_names_soundex,id[:-1], True))

def has_wrd(id, wrds, dom, tld, complete_address):
    return int(is_in_index(allwrds,id[1:]) or
               is_in_index(allwrds,id[:-1]) or
               is_in_index(allwrds,id[:-1].rstrip('s')) or
               is_in_index(allwrds,id[1:].rstrip('s')))


def has_any_name(id, wrds, dom, tld, complete_address):
    return any(map(lambda x: is_in_index(names,x), wrds))

def are_all_names(id, wrds, dom, tld, complete_address):
    return all(map(lambda x: is_in_index(names,x), wrds))

def has_any_dictionary_word(id, wrds, dom, tld, complete_address):
    return any(map(lambda x: is_in_index(allwrds,x), wrds))

def are_all_dictionary_words(id, wrds, dom, tld, complete_address):
    return all(map(lambda x: is_in_index(allwrds,x), wrds))

def is_group_email(id, wrds, dom, tld, complete_address):
    return is_in_index(group_email_domains,dom)


def is_common_email_host(id, wrds, dom, tld, complete_address):
    return is_in_index(common_email_domains,dom)

def is_org_edu_tld(id, wrds, dom, tld, complete_address):
    return tld == 'org' or tld == 'edu'

def is_info_me_tld(id, wrds, dom, tld, complete_address):
    return tld == 'me' or tld == 'info'

def domain_in_id_or_id_in_domain(id, wrds, dom, tld, complete_address):
    return dom in wrds or id == dom or dom.find(id) > -1 or id.find(dom) > -1 or any(map(lambda wrd: dom.find(wrd)>-1,wrds))

def has_number_in_id(id, wrds, dom, tld, complete_address):
    return re.search("[0-9]", id) != None

def has_subdomins(id, wrds, dom, tld, complete_address):
    return len(complete_address.split('@')[-1].split('.')) > 2

def parseemail(email_id):
    [id, dom] = email_id.split('@')
    [dom, tld] = dom.split('.')[-2:]
    id = clean_id(id)
    return [id, dom, tld]

def get_features(email_id, sent, recd, name):
    [id,dom,tld] = parseemail(email_id)
    if name:
        name = name.replace("%20",' ')
        words_in_id = name.strip().replace(',','').lower().split(' ')
    else:
        words_in_id = id_words(id)
    fns = [has_name, has_wrd, has_any_name, are_all_names, has_any_dictionary_word, are_all_dictionary_words, is_group_email, is_common_email_host, is_org_edu_tld, is_info_me_tld, domain_in_id_or_id_in_domain, has_number_in_id, has_subdomins]
    features = dict(map(lambda x: (x.__name__, int(x(id, words_in_id, dom, tld, email_id))), fns))
    print features
    features['sent'] = log(1+sent)
    features['recvd'] = log(1+recd)
    features['has_name_given'] = int(name != None)
    return features


def read_json(fl):
    return json.loads('\n'.join(open(fl).readlines()))

def feature_extraction():
    global email, features
    for email in read_json('testdata.json'):
        try:
            email_id = email['email']
            sent = email["sent_count"]
            recd = email["received_count"]
            name = email.get("name",None)
            features = get_features(email_id.strip(), int(sent), int(recd), name)
            print features + ',1    '
        except:
            pass

    # for email in read_json('pos_json'):
    #     try:
    #         email_id = email['email']
    #         sent = email["sent_count"]
    #         recd = email["received_count"]
    #         name = email.get("name",None)
    #         features = get_features(email_id.strip(), int(sent), int(recd), name)
    #         print features + ',1'
    #     except:
    #         pass
    # for email in read_json('neg_json'):
    #     try:
    #         email_id = email['email']
    #         sent = email["sent_count"]
    #         recd = email["received_count"]
    #         name = email.get("name",None)
    #         features = get_features(email_id.strip(), int(sent), int(recd), name)
    #         print features + ',0'
    #     except:
    #         pass


def classify(email_id, sent_count = 0, received_count = 0, name = None):
    coeff = {"has_name":1.085958,"has_wrd":1.472036e-04,"has_any_name":3.376846e-01,"are_all_names":4.140926e-01,"has_any_dictionary_word":-3.370337e-01,"are_all_dictionary_words":-5.085958e-01,"is_group_email":-3.266195e+00,"is_common_email_host":1.662579e+00,"is_org_edu_tld":0.000000e+00,"domain_in_id_or_id_in_domain":-3.379769e-01,"has_number_in_id":-1.032386e-03,"has_subdomins":5.353531e-05,"sent":6.135196e-01,"recvd":-2.246330e-04,"has_name_given":5.085958e-01, "intercept":-1.016358}
    features = get_features(email_id,sent_count,received_count,name)
    features["intercept"] = 1
    return dict([(email_id, reduce(add,map(lambda x: coeff[x]*features[x], coeff.keys()),0))])

class Handler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_GET(self):
        try:
            self.send_response(200)
            self.send_header("Content-type", "text/plain")
            self.end_headers()
            path = self.path
            if path.find('?') > 1:
                params = dict(map(lambda x: x.split('='),path.split('?')[1].split('&')))
                email = params.get('email')
                print params
                if email:
                    print email
                    result = classify(email,float(params.get('sent_count', 0)), float(params.get('received_count',0)), params.get('name'))
                    print json.dumps(result)
                    self.wfile.write(json.dumps(result))
            self.wfile.write("")
        except:
            self.wfile.write("")
        self.wfile.close()


class MultiThreadedHTTPServer(ThreadingMixIn, BaseHTTPServer.HTTPServer):
    pass

def run(server_class=MultiThreadedHTTPServer,
        handler_class=BaseHTTPServer.BaseHTTPRequestHandler):
    server_address = ('0.0.0.0', 8080)
    httpd = server_class(server_address, handler_class)
    httpd.serve_forever()

if __name__ == '__main__':
    run(handler_class=Handler)
    classify(*sys.argv[1:])
    #feature_extraction()
    #print classify("order.update@amazon.com")

class TestUtils(unittest.TestCase):

    def test_segmentation(self):
        self.assertTrue(segment("indiaisabigcountry",set(["india","is","a","big","country","count","in","dia", "i"])))
        self.assertTrue(segment("carissafe",allwrds))
        self.assertTrue(segment("isabeautifulcountry",allwrds))
        self.assertFalse(segment("amitrathore",allwrds))
        self.assertFalse(segment("karthikk",allwrds))
        self.assertFalse(segment("karthikkumara",allwrds))
        self.assertFalse(segment("sivajagadeesan",allwrds))
        self.assertFalse(segment("robberger",allwrds))
        self.assertFalse(segment("alejandrolopez",allwrds))


    def test_segmenting_names(self):
        self.assertFalse(segment("indiaisabigcountry",names))
        self.assertTrue(segment("robberger",names))
        self.assertTrue(segment("karthikkumara",names))
        self.assertFalse(segment("orderupdate",names))
        self.assertFalse(segment("updates",names))
        self.assertTrue(segment("sivajagadeesan",names))
        self.assertFalse(segment("groupsupdates",names))
        self.assertTrue(segment("alejandrolopez",names))
        self.assertFalse(segment("donotreply",names))
        self.assertFalse(segment("localupdates",names))
        self.assertFalse(segment("newsletter",names))
        self.assertFalse(segment("donotreply",names))

    def test_splitting_id(self):
        self.assertEquals(["amit","rathore"], id_words("amit.rathore"))
        self.assertEquals(["siva", "jag"], id_words("SivaJag"))
        self.assertEquals(["karthik","kumara"], id_words("karthik_kumara"))
        self.assertEquals(["order","update"], id_words("order-update"))
        self.assertEquals(["amit","rathore"], id_words("amitrathore"))
        self.assertEquals(["siva", "jag"], id_words("sivajag"))
        self.assertEquals(["karthik","kumara"], id_words("karthikkumara"))
        self.assertEquals(["order","update"], id_words("orderupdate"))


    def test_getting_features(self):
        [id,dom,tld] = parseemail('sourceforge@newsletters.sourceforge.net')
        self.assertTrue(domain_in_id_or_id_in_domain(id,id_words(id),dom,tld, None))
        [id,dom,tld] = parseemail('americanexpress@email2.americanexpress.com')
        self.assertTrue(domain_in_id_or_id_in_domain(id,id_words(id),dom,tld, None))
        [id,dom,tld] = parseemail('att_update@amcustomercare.att-mail.com')
        self.assertTrue(domain_in_id_or_id_in_domain(id,id_words(id),dom,tld, None))
        [id,dom,tld] = parseemail('siva@yewoh.com')
        self.assertFalse(are_all_dictionary_words(id,id_words(id),dom,tld, None))
        [id,dom,tld] = parseemail('mymileageplus@news.united.com')
        self.assertTrue(are_all_dictionary_words(id,id_words(id),dom,tld, None))
        [id,dom,tld] = parseemail('mymileageplus@news.united.com')
        self.assertTrue(has_subdomins(id,id_words(id),dom,tld, 'mymileageplus@news.united.com'))
