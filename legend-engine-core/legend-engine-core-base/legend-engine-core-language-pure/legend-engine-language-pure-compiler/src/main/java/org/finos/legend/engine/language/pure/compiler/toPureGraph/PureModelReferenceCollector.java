// Copyright 2024 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.multimap.set.SetMultimap;
import org.eclipse.collections.impl.multimap.set.SynchronizedPutUnifiedSetMultimap;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public interface PureModelReferenceCollector
{
    PureModelReferenceCollector NO_OP_COLLECTOR = new PureModelReferenceCollectorNoOp();

    void register(SourceInformation reference, CoreInstance referenced);

    SetMultimap<CoreInstance, SourceInformation> references();


    class PureModelReferenceCollectorImpl implements PureModelReferenceCollector
    {
        private final SynchronizedPutUnifiedSetMultimap<CoreInstance, SourceInformation> multimap = SynchronizedPutUnifiedSetMultimap.newMultimap();

        @Override
        public void register(SourceInformation reference, CoreInstance referenced)
        {
            if (reference != null
                && !reference.equals(SourceInformation.getUnknownSourceInformation())
                && referenced.getSourceInformation() != null
                && !referenced.getSourceInformation().equals(SourceInformationHelper.toM3SourceInformation(reference))
            )
            {
                this.multimap.put(referenced, reference);
            }
        }

        @Override
        public SetMultimap<CoreInstance, SourceInformation> references()
        {
            return this.multimap;
        }
    }

    class PureModelReferenceCollectorNoOp implements PureModelReferenceCollector
    {
        private PureModelReferenceCollectorNoOp()
        {

        }

        @Override
        public void register(SourceInformation reference, CoreInstance referenced)
        {

        }

        @Override
        public SetMultimap<CoreInstance, SourceInformation> references()
        {
            throw new UnsupportedOperationException("No reference collected.  Make sure collection was enable.");
        }
    }
}
