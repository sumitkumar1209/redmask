package com.hashedin.redmask.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hashedin.redmask.common.MaskingRuleDef;
import com.hashedin.redmask.exception.RedmaskConfigException;

/**
 * This factory class is used to create an instance of a specific masking rule from the inputted
 * column rules.
 */
public class MaskingRuleFactory {

  private static final Logger log = LoggerFactory.getLogger(MaskingRuleFactory.class);
  
  public MaskingRuleDef getColumnMaskingRule(MaskingRuleDef ruleDef) {

    MaskingRuleDef specificRule = null;
    try {
      specificRule = ruleDef.getMaskType().getClassType().newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RedmaskConfigException("Invalid MaskType or function class specified.", e);
    }
    specificRule.setColumnName(ruleDef.getColumnName());
    specificRule.setMaskType(ruleDef.getMaskType());
    specificRule.setMaskParams(ruleDef.getMaskParams());
    return specificRule;
  }

}