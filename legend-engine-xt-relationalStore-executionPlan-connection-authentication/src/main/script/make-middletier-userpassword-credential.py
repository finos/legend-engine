#!/bin/env python

import sys
import json

user = sys.argv[1]
password = sys.argv[2]
keytab_usage_contexts = sys.argv[3:]

metadata = {
    'user' : user,
    'password' : password,
    'usageContexts' : keytab_usage_contexts
}

print(json.dumps(metadata, indent = 4))
