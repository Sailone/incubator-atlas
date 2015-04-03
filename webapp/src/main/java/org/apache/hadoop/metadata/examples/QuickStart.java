/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.metadata.examples;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.hadoop.metadata.MetadataServiceClient;
import org.apache.hadoop.metadata.typesystem.Referenceable;
import org.apache.hadoop.metadata.typesystem.TypesDef;
import org.apache.hadoop.metadata.typesystem.json.InstanceSerialization;
import org.apache.hadoop.metadata.typesystem.json.TypesSerialization;
import org.apache.hadoop.metadata.typesystem.persistence.Id;
import org.apache.hadoop.metadata.typesystem.types.AttributeDefinition;
import org.apache.hadoop.metadata.typesystem.types.ClassType;
import org.apache.hadoop.metadata.typesystem.types.DataTypes;
import org.apache.hadoop.metadata.typesystem.types.EnumTypeDefinition;
import org.apache.hadoop.metadata.typesystem.types.HierarchicalTypeDefinition;
import org.apache.hadoop.metadata.typesystem.types.IDataType;
import org.apache.hadoop.metadata.typesystem.types.Multiplicity;
import org.apache.hadoop.metadata.typesystem.types.StructTypeDefinition;
import org.apache.hadoop.metadata.typesystem.types.TraitType;
import org.apache.hadoop.metadata.typesystem.types.TypeUtils;
import org.apache.hadoop.metadata.typesystem.types.utils.TypesUtil;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A driver that sets up sample types and data for testing purposes.
 * Please take a look at QueryDSL in docs for the Meta Model.
 * todo - move this to examples module.
 */
public class QuickStart {

    public static void main(String[] args) throws Exception {
        String baseUrl = getServerUrl(args);
        QuickStart quickStart = new QuickStart(baseUrl);

        // Shows how to create types in DGI for your meta model
        quickStart.createTypes();

        // Shows how to create entities (instances) for the added types in DGI
        quickStart.createEntities();

        // Shows some search queries using DSL based on types
        quickStart.search();
    }

    static String getServerUrl(String[] args) {
        String baseUrl = "http://localhost:21000";
        if (args.length > 0) {
            baseUrl = args[0];
        }

        return baseUrl;
    }

    private static final String DATABASE_TYPE = "DB";
    private static final String COLUMN_TYPE = "Column";
    private static final String TABLE_TYPE = "Table";
    private static final String VIEW_TYPE = "View";
    private static final String LOAD_PROCESS_TYPE = "hive_process";
    private static final String STORAGE_DESC_TYPE = "StorageDesc";

    private static final String[] TYPES = {
            DATABASE_TYPE, TABLE_TYPE, STORAGE_DESC_TYPE, COLUMN_TYPE, LOAD_PROCESS_TYPE, VIEW_TYPE,
            "JdbcAccess", "ETL", "Metric", "PII", "Fact", "Dimension"
    };

    private final MetadataServiceClient metadataServiceClient;

    QuickStart(String baseUrl) {
        metadataServiceClient = new MetadataServiceClient(baseUrl);
    }

    void createTypes() throws Exception {
        TypesDef typesDef = createTypeDefinitions();

        String typesAsJSON = TypesSerialization.toJson(typesDef);
        System.out.println("typesAsJSON = " + typesAsJSON);
        metadataServiceClient.createType(typesAsJSON);

        // verify types created
        verifyTypesCreated();
    }

    TypesDef createTypeDefinitions() throws Exception {
        HierarchicalTypeDefinition<ClassType> dbClsDef
                = TypesUtil.createClassTypeDef(DATABASE_TYPE, null,
                attrDef("name", DataTypes.STRING_TYPE),
                attrDef("description", DataTypes.STRING_TYPE),
                attrDef("locationUri", DataTypes.STRING_TYPE),
                attrDef("owner", DataTypes.STRING_TYPE),
                attrDef("createTime", DataTypes.INT_TYPE)
        );

        HierarchicalTypeDefinition<ClassType> storageDescClsDef =
                TypesUtil.createClassTypeDef(STORAGE_DESC_TYPE, null,
                        attrDef("location", DataTypes.STRING_TYPE),
                        attrDef("inputFormat", DataTypes.STRING_TYPE),
                        attrDef("outputFormat", DataTypes.STRING_TYPE),
                        attrDef("compressed", DataTypes.STRING_TYPE,
                                Multiplicity.REQUIRED, false, null)
                );

        HierarchicalTypeDefinition<ClassType> columnClsDef =
                TypesUtil.createClassTypeDef(COLUMN_TYPE, null,
                        attrDef("name", DataTypes.STRING_TYPE),
                        attrDef("dataType", DataTypes.STRING_TYPE),
                        attrDef("comment", DataTypes.STRING_TYPE)
                );

        HierarchicalTypeDefinition<ClassType> tblClsDef =
                TypesUtil.createClassTypeDef(TABLE_TYPE, null,
                        attrDef("name", DataTypes.STRING_TYPE),
                        attrDef("description", DataTypes.STRING_TYPE),
                        new AttributeDefinition("db", DATABASE_TYPE,
                                Multiplicity.REQUIRED, false, null),
                        new AttributeDefinition("sd", STORAGE_DESC_TYPE,
                                Multiplicity.REQUIRED, false, null),
                        attrDef("owner", DataTypes.STRING_TYPE),
                        attrDef("createTime", DataTypes.INT_TYPE),
                        attrDef("lastAccessTime", DataTypes.INT_TYPE),
                        attrDef("retention", DataTypes.INT_TYPE),
                        attrDef("viewOriginalText", DataTypes.STRING_TYPE),
                        attrDef("viewExpandedText", DataTypes.STRING_TYPE),
                        attrDef("tableType", DataTypes.STRING_TYPE),
                        attrDef("temporary", DataTypes.BOOLEAN_TYPE),
                        new AttributeDefinition("columns",
                                DataTypes.arrayTypeName(COLUMN_TYPE),
                                Multiplicity.COLLECTION, true, null)
                );

        HierarchicalTypeDefinition<ClassType> loadProcessClsDef =
                TypesUtil.createClassTypeDef(LOAD_PROCESS_TYPE, null,
                        attrDef("name", DataTypes.STRING_TYPE),
                        attrDef("userName", DataTypes.STRING_TYPE),
                        attrDef("startTime", DataTypes.INT_TYPE),
                        attrDef("endTime", DataTypes.INT_TYPE),
                        new AttributeDefinition("inputTables",
                                DataTypes.arrayTypeName(TABLE_TYPE),
                                Multiplicity.COLLECTION, false, null),
                        new AttributeDefinition("outputTables",
                                DataTypes.arrayTypeName(TABLE_TYPE),
                                Multiplicity.COLLECTION, false, null),
                        attrDef("queryText", DataTypes.STRING_TYPE, Multiplicity.REQUIRED),
                        attrDef("queryPlan", DataTypes.STRING_TYPE, Multiplicity.REQUIRED),
                        attrDef("queryId", DataTypes.STRING_TYPE, Multiplicity.REQUIRED),
                        attrDef("queryGraph", DataTypes.STRING_TYPE, Multiplicity.REQUIRED)
                );

        HierarchicalTypeDefinition<ClassType> viewClsDef =
                TypesUtil.createClassTypeDef(VIEW_TYPE, null,
                        attrDef("name", DataTypes.STRING_TYPE),
                        new AttributeDefinition("db", DATABASE_TYPE,
                                Multiplicity.REQUIRED, false, null),
                        new AttributeDefinition("inputTables",
                                DataTypes.arrayTypeName(TABLE_TYPE),
                                Multiplicity.COLLECTION, false, null)
                );

        HierarchicalTypeDefinition<TraitType> dimTraitDef =
                TypesUtil.createTraitTypeDef("Dimension", null);

        HierarchicalTypeDefinition<TraitType> factTraitDef =
                TypesUtil.createTraitTypeDef("Fact", null);

        HierarchicalTypeDefinition<TraitType> piiTraitDef =
                TypesUtil.createTraitTypeDef("PII", null);

        HierarchicalTypeDefinition<TraitType> metricTraitDef =
                TypesUtil.createTraitTypeDef("Metric", null);

        HierarchicalTypeDefinition<TraitType> etlTraitDef =
                TypesUtil.createTraitTypeDef("ETL", null);

        HierarchicalTypeDefinition<TraitType> jdbcTraitDef =
                TypesUtil.createTraitTypeDef("JdbcAccess", null);

        return TypeUtils.getTypesDef(
                ImmutableList.<EnumTypeDefinition>of(),
                ImmutableList.<StructTypeDefinition>of(),
                ImmutableList.of(dimTraitDef, factTraitDef,
                        piiTraitDef, metricTraitDef, etlTraitDef, jdbcTraitDef),
                ImmutableList.of(dbClsDef, storageDescClsDef, columnClsDef,
                        tblClsDef, loadProcessClsDef, viewClsDef)
        );
    }

    AttributeDefinition attrDef(String name, IDataType dT) {
        return attrDef(name, dT, Multiplicity.OPTIONAL, false, null);
    }

    AttributeDefinition attrDef(String name, IDataType dT, Multiplicity m) {
        return attrDef(name, dT, m, false, null);
    }

    AttributeDefinition attrDef(String name, IDataType dT,
                                Multiplicity m, boolean isComposite, String reverseAttributeName) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(dT);
        return new AttributeDefinition(name, dT.getName(), m, isComposite, reverseAttributeName);
    }

    void createEntities() throws Exception {
        Id salesDB = database(
                "Sales", "Sales Database", "John ETL", "hdfs://host:8000/apps/warehouse/sales");


        Referenceable sd = rawStorageDescriptor("hdfs://host:8000/apps/warehouse/sales",
                "TextInputFormat", "TextOutputFormat", true);

        ArrayList<Referenceable> salesFactColumns = new ArrayList<>();
        salesFactColumns.add(rawColumn("time_id", "int", "time id"));
        salesFactColumns.add(rawColumn("product_id", "int", "product id"));
        salesFactColumns.add(rawColumn("customer_id", "int", "customer id", "PII"));
        salesFactColumns.add(rawColumn("sales", "double", "product id", "Metric"));

        Id salesFact = table("sales_fact", "sales fact table",
                salesDB, sd, "Joe", "Managed", salesFactColumns, "Fact");

        ArrayList<Referenceable> productDimColumns = new ArrayList<>();
        productDimColumns.add(rawColumn("product_id", "int", "product id"));
        productDimColumns.add(rawColumn("product_name", "string", "product name"));
        productDimColumns.add(rawColumn("brand_name", "int", "brand name"));

        Id productDim = table("product_dim", "product dimension table",
                salesDB, sd, "John Doe", "Managed", productDimColumns, "Dimension");

        ArrayList<Referenceable> timeDimColumns = new ArrayList<>();
        timeDimColumns.add(rawColumn("time_id", "int", "time id"));
        timeDimColumns.add(rawColumn("dayOfYear", "int", "day Of Year"));
        timeDimColumns.add(rawColumn("weekDay", "int", "week Day"));

        Id timeDim = table("time_dim", "time dimension table",
                salesDB, sd, "John Doe", "External", timeDimColumns, "Dimension");


        ArrayList<Referenceable> customerDimColumns = new ArrayList<>();
        customerDimColumns.add(rawColumn("customer_id", "int", "customer id", "PII"));
        customerDimColumns.add(rawColumn("name", "string", "customer name", "PII"));
        customerDimColumns.add(rawColumn("address", "string", "customer address", "PII"));

        Id customerDim = table("customer_dim", "customer dimension table",
                salesDB, sd, "fetl", "External", customerDimColumns, "Dimension");


        Id reportingDB = database("Reporting", "reporting database", "Jane BI",
                "hdfs://host:8000/apps/warehouse/reporting");

        Id salesFactDaily = table("sales_fact_daily_mv",
                "sales fact daily materialized view", reportingDB, sd,
                "Joe BI", "Managed", salesFactColumns, "Metric");

        Id loadSalesFactDaily = loadProcess("loadSalesDaily", "John ETL",
                ImmutableList.of(salesFact, timeDim), ImmutableList.of(salesFactDaily),
                "create table as select ", "plan", "id", "graph",
                "ETL");
        System.out.println("added loadSalesFactDaily = " + loadSalesFactDaily);

        Id productDimView = view("product_dim_view", reportingDB,
                ImmutableList.of(productDim), "Dimension", "JdbcAccess");
        System.out.println("added productDimView = " + productDimView);

        Id customerDimView = view("customer_dim_view", reportingDB,
                ImmutableList.of(customerDim), "Dimension", "JdbcAccess");
        System.out.println("added customerDimView = " + customerDimView);

        Id salesFactMonthly = table("sales_fact_monthly_mv",
                "sales fact monthly materialized view",
                reportingDB, sd, "Jane BI", "Managed", salesFactColumns, "Metric");

        Id loadSalesFactMonthly = loadProcess("loadSalesMonthly", "John ETL",
                ImmutableList.of(salesFactDaily), ImmutableList.of(salesFactMonthly),
                "create table as select ", "plan", "id", "graph",
                "ETL");
        System.out.println("added loadSalesFactMonthly = " + loadSalesFactMonthly);
    }

    private Id createInstance(Referenceable referenceable) throws Exception {
        String typeName = referenceable.getTypeName();

        String entityJSON = InstanceSerialization.toJson(referenceable, true);
        System.out.println("Submitting new entity= " + entityJSON);
        JSONObject jsonObject = metadataServiceClient.createEntity(entityJSON);
        String guid = jsonObject.getString(MetadataServiceClient.RESULTS);
        System.out.println("created instance for type " + typeName + ", guid: " + guid);

        // return the Id for created instance with guid
        return new Id(guid, referenceable.getId().getVersion(), referenceable.getTypeName());
    }

    Id database(String name, String description,
                           String owner, String locationUri,
                           String... traitNames) throws Exception {
        Referenceable referenceable = new Referenceable(DATABASE_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set("description", description);
        referenceable.set("owner", owner);
        referenceable.set("locationUri", locationUri);
        referenceable.set("createTime", System.currentTimeMillis());

        return createInstance(referenceable);
    }

    Referenceable rawStorageDescriptor(String location, String inputFormat,
                                       String outputFormat,
                                       boolean compressed) throws Exception {
        Referenceable referenceable = new Referenceable(STORAGE_DESC_TYPE);
        referenceable.set("location", location);
        referenceable.set("inputFormat", inputFormat);
        referenceable.set("outputFormat", outputFormat);
        referenceable.set("compressed", compressed);

        return referenceable;
    }

    Referenceable rawColumn(String name, String dataType, String comment,
                            String... traitNames) throws Exception {
        Referenceable referenceable = new Referenceable(COLUMN_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set("dataType", dataType);
        referenceable.set("comment", comment);

        return referenceable;
    }

    Id table(String name, String description,
             Id dbId, Referenceable sd,
             String owner, String tableType,
             List<Referenceable> columns,
             String... traitNames) throws Exception {
        Referenceable referenceable = new Referenceable(TABLE_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set("description", description);
        referenceable.set("owner", owner);
        referenceable.set("tableType", tableType);
        referenceable.set("createTime", System.currentTimeMillis());
        referenceable.set("lastAccessTime", System.currentTimeMillis());
        referenceable.set("retention", System.currentTimeMillis());
        referenceable.set("db", dbId);
        referenceable.set("sd", sd);
        referenceable.set("columns", columns);

        return createInstance(referenceable);
    }

    Id loadProcess(String name, String user,
                   List<Id> inputTables,
                   List<Id> outputTables,
                   String queryText, String queryPlan,
                   String queryId, String queryGraph,
                   String... traitNames) throws Exception {
        Referenceable referenceable = new Referenceable(LOAD_PROCESS_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set("user", user);
        referenceable.set("startTime", System.currentTimeMillis());
        referenceable.set("endTime", System.currentTimeMillis() + 10000);

        referenceable.set("inputTables", inputTables);
        referenceable.set("outputTables", outputTables);

        referenceable.set("queryText", queryText);
        referenceable.set("queryPlan", queryPlan);
        referenceable.set("queryId", queryId);
        referenceable.set("queryGraph", queryGraph);

        return createInstance(referenceable);
    }

    Id view(String name, Id dbId,
            List<Id> inputTables,
            String... traitNames) throws Exception {
        Referenceable referenceable = new Referenceable(VIEW_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set("db", dbId);

        referenceable.set("inputTables", inputTables);

        return createInstance(referenceable);
    }

    private void verifyTypesCreated() throws Exception {
        List<String> types = metadataServiceClient.listTypes();
        for (String type : TYPES) {
            assert types.contains(type);
        }
    }

    private String[] getDSLQueries() {
        return new String[]{
            "from DB",
            "DB",
            "DB where name=\"Reporting\"",
            "DB where DB.name=\"Reporting\"",
            "DB name = \"Reporting\"",
            "DB DB.name = \"Reporting\"",
            "DB where name=\"Reporting\" select name, owner",
            "DB where DB.name=\"Reporting\" select name, owner",
            "DB has name",
            "DB where DB has name",
            "DB, Table",
            "DB is JdbcAccess",
            /*
            "DB, hive_process has name",
            "DB as db1, Table where db1.name = \"Reporting\"",
            "DB where DB.name=\"Reporting\" and DB.createTime < " + System.currentTimeMillis()},
            */
            "from Table",
            "Table",
            "Table is Dimension",
            "Column where Column isa PII",
            "View is Dimension",
            /*"Column where Column isa PII select Column.name",*/
            "Column select Column.name",
            "Column select name",
            "Column where Column.name=\"customer_id\"",
            "from Table select Table.name",
            "DB where (name = \"Reporting\")",
            "DB where (name = \"Reporting\") select name as _col_0, owner as _col_1",
            "DB where DB is JdbcAccess",
            "DB where DB has name",
            "DB Table",
            "DB where DB has name",
            "DB as db1 Table where (db1.name = \"Reporting\")",
            "DB where (name = \"Reporting\") select name as _col_0, (createTime + 1) as _col_1 ",
            /*
            todo: does not work
            "DB where (name = \"Reporting\") and ((createTime + 1) > 0)",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) and (db1.name = \"Reporting\") select db1.name as dbName, tab.name as tabName",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) or (db1.name = \"Reporting\") select db1.name as dbName, tab.name as tabName",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) and (db1.name = \"Reporting\") or db1 has owner select db1.name as dbName, tab.name as tabName",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) and (db1.name = \"Reporting\") or db1 has owner select db1.name as dbName, tab.name as tabName",
            */
            // trait searches
            "Dimension",
            /*"Fact", - todo: does not work*/
            "JdbcAccess",
            "ETL",
            "Metric",
            "PII",
            /*
            // Lineage - todo - fix this, its not working
            "Table hive_process outputTables",
            "Table loop (hive_process outputTables)",
            "Table as _loop0 loop (hive_process outputTables) withPath",
            "Table as src loop (hive_process outputTables) as dest select src.name as srcTable, dest.name as destTable withPath",
            */
            "Table as t, columns where t.name=\"sales_fact\"",
        };
    }

    private void search() throws Exception {
        for (String dslQuery : getDSLQueries()) {
            JSONObject response = metadataServiceClient.searchEntity(dslQuery);
            JSONObject results = response.getJSONObject(MetadataServiceClient.RESULTS);
            if (!results.isNull("rows")) {
                JSONArray rows = results.getJSONArray("rows");
                System.out.println("query [" + dslQuery + "] returned [" + rows.length() + "] rows");
            } else {
                System.out.println("query [" + dslQuery + "] failed, results:" + results.toString());
            }
        }
    }
}