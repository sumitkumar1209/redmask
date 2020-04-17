package com.hashedin.redmask.service;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import org.apache.logging.log4j.util.Strings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashedin.redmask.configurations.*;

import freemarker.template.TemplateException;

public class QueryBuilderUtil {

  private static final String NEW_LINE = System.getProperty("line.separator");
  private static final String SELECT_QUERY = "SELECT * FROM ";

  public static void buildFunctionsAndQueryForView(MaskingRule rule, FileWriter writer,
      MaskConfiguration config, String url)
          throws SQLException, IOException, InstantiationException, IllegalAccessException, TemplateException {

    Set<String> functionDefinitionSet = new LinkedHashSet<>();
    List<String> querySubstring = new ArrayList<>();
    ResultSetMetaData rs = null;

    // get all columns of given table.
    String query = SELECT_QUERY + rule.getTable();

    try(Connection conn = DriverManager.getConnection(url,
        config.getSuperUser(), config.getSuperUserPassword());
        Statement st = conn.createStatement()) {
      rs = st.executeQuery(query).getMetaData();
    }

    MaskingRuleFactory columnRuleFactory = new MaskingRuleFactory();

    Map<String, MaskingRuleDef> colMaskRuleMap = new HashMap<>();
    for (ColumnRule col : rule.getColumns()) {
      // Build MaskingRuleDef object.
      MaskingRuleDef def = buildMaskingRuleDef(col);
      colMaskRuleMap.put(col.getColumnName(), columnRuleFactory.getColumnMaskingRule(def));
    }

    // TODO: Add validation, if column to be masked does not exists.

    // Dynamically build sub query part for create view.
    for (int i = 1; i <= rs.getColumnCount(); i++) {
      String colName = rs.getColumnName(i);
      if (colMaskRuleMap.containsKey(colName)) {        
        querySubstring.add(colMaskRuleMap.get(colName).getSubQuery(rule.getTable()));
        colMaskRuleMap.get(colName).addFunctionDefinition(config, functionDefinitionSet);
      } else {
        querySubstring.add(rs.getColumnName(i));
      }
    }

    for (String functionDefinition: functionDefinitionSet) {
      writer.append(functionDefinition);
    }

    // Create view
    String queryString = String.join(",", querySubstring);
    StringBuilder sb = new StringBuilder();
    sb.append(config.getUsername()).append(".").append(rule.getTable());

    String createViewQuery = "CREATE VIEW " + sb.toString() + " AS SELECT " +
        queryString +  " FROM " + rule.getTable() + ";";

    writer.append("\n\n-- Create masked view.\n");
    writer.append(createViewQuery);
  }

  public static String dropSchemaQuery(String schemaName) {
    StringBuilder sb = new StringBuilder();
    sb.append(NEW_LINE)
    .append("-- Drop " + schemaName + "Schema if it exists.")
    .append(NEW_LINE);

    sb.append("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE;")
    .append(NEW_LINE);
    return sb.toString();
  }

  public static String createSchemaQuery(String schemaName) {
    StringBuilder sb = new StringBuilder();
    sb.append(NEW_LINE)
    .append("-- Create " + schemaName + " schema.")
    .append(NEW_LINE);

    sb.append("CREATE SCHEMA IF NOT EXISTS " + schemaName + ";")
    .append(NEW_LINE);
    return sb.toString();
  }

  private static MaskingRuleDef buildMaskingRuleDef(ColumnRule colRule) {
    Map<String, String> maskParams = new ObjectMapper().
        convertValue(colRule.getMaskParams(),
            new TypeReference<Map<String, String>>(){});

    return new MaskingRuleDef(colRule.getColumnName(),
        colRule.getMaskType(), maskParams) {

      @Override
      public void addFunctionDefinition(
          MaskConfiguration config,
          Set<String> funcSet) {
      }

      @Override
      public String getSubQuery(String tableName) {
        return Strings.EMPTY;
      }
    };
  }

}
