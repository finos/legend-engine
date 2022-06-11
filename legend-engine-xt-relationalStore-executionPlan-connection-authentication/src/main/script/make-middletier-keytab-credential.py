#!/bin/env python

import sys
import json

keytab_vault_reference = sys.argv[1]
keytab_usage_contexts = sys.argv[2:]

metadata = {
    'keytabReference' : keytab_vault_reference,
    'usageContexts' : keytab_usage_contexts
}

print(json.dumps(metadata, indent = 4))
