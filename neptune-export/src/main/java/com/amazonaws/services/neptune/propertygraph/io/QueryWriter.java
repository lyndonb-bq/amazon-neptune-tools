/*
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
A copy of the License is located at
    http://www.apache.org/licenses/LICENSE-2.0
or in the "license" file accompanying this file. This file is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing
permissions and limitations under the License.
*/

package com.amazonaws.services.neptune.propertygraph.io;

import java.io.IOException;
import java.util.Map;

public class QueryWriter implements LabelWriter<Map<?, ?>> {

    private final PropertyGraphPrinter propertyGraphPrinter;

    public QueryWriter(PropertyGraphPrinter propertyGraphPrinter) {
        this.propertyGraphPrinter = propertyGraphPrinter;
    }

    @Override
    public void handle(Map<?, ?> properties, boolean allowTokens) throws IOException {

        propertyGraphPrinter.printStartRow();
        propertyGraphPrinter.printProperties(properties);
        propertyGraphPrinter.printEndRow();
    }

    @Override
    public void close() throws Exception {
        propertyGraphPrinter.close();
    }

    @Override
    public String outputId() {
        return propertyGraphPrinter.outputId();
    }
}