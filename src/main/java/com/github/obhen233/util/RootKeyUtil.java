package com.github.obhen233.util;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.github.obhen233.element.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.obhen233.annotation.framework.NoBase;
import com.github.obhen233.annotation.framework.NoParam;
import com.github.obhen233.annotation.framework.RuleBase;
import com.github.obhen233.annotation.framework.RuleParam;
import com.github.obhen233.attribute.Nrule;
import com.github.obhen233.attribute.Rule;
import com.github.obhen233.element.Leaf;
import com.github.obhen233.element.Node;
import com.github.obhen233.element.Root;
import com.github.obhen233.exception.ClassUnregisteredException;
import com.github.obhen233.producer.RuleProducer;

public class RootKeyUtil {
	
	private static Logger logger = LoggerFactory.getLogger(RootKeyUtil.class);
	
	public static String getkeyFromMap(Map param,String unid){
		StringBuilder sb = new StringBuilder();
		sb.append(unid);
		SortedMap<String,Object> sortMap = new TreeMap<String,Object>();
		sortMap.putAll(param);
		for(Object key : sortMap.keySet()){
			if(!"unid".equals(key))
				sb.append("&").append(key);
		}
		return CodingUtil.MD5(sb.toString());
	}
	
	

	
	public static Root setRootFields(Root root,Map<String,Object> param) throws Exception{
		 setParamFields(root.getElements(),param);
		 return root;
	}
	
	private static void setParamFields(List<Element> elements, Map<String,Object> param){
		for(Element element:elements){
			if(Node.class.isInstance(element)){
				setParamFields(((Node)element).getElements(),param);
			}else if(Leaf.class.isInstance(element)){
				List<Rule> rules = ((Leaf)element).getRules();
				if(rules != null && rules.size() > 0){
					for(Rule rule:rules){
						if(Nrule.class.isInstance(rule)){
							Rule nr = ((Nrule)rule).getRule();
							setParamField(nr,param);
						}else{
							setParamField(rule,param);
						}	
					}
				}
			}
		}
	}
	
	private static void setParamField(Rule rule,Map<String,Object> param) {
		Class clazz = rule.getClass();
		
		Field[] fields = StringUtil.getAllFields(clazz);
		String preKey = RuleProducer.getKeyByClassName(clazz.getName());
		if(StringUtil.isBlank(preKey))
			throw new ClassUnregisteredException("Rule Class \""+clazz.getName()+"\" unregistered");
		StringBuilder s = null;
		for(Field f : fields){
			f.setAccessible(true);
			String name = f.getName();
			Object o = ReflectionUtil.invokeGet(rule,name);
			if(o == null){
				RuleParam ruleParam = f.getDeclaredAnnotation(RuleParam.class);
				RuleBase ruleBase = f.getDeclaredAnnotation(RuleBase.class);
				NoParam noParam = (NoParam)clazz.getDeclaredAnnotation(NoParam.class);
				NoBase noBase = (NoBase)clazz.getDeclaredAnnotation(NoBase.class);
				if(ruleParam != null && noParam == null){
					String value = ruleParam.value();
					String key = StringUtil.isNotBlank(value)?value:name;
					s = new StringBuilder();
					Object po = param.get(s.append(preKey).append(".").append(key).toString());
					if(po == null){
						logger.info(s.toString() +"not find value from paramMap");
					}else{
						ReflectionUtil.invokeSet(rule,name,StringUtil.StringToField((String)po, f));
					}
				}else if(ruleBase != null && noBase == null){
					String value = ruleBase.value();
					String key = StringUtil.isNotBlank(value)?value:name;
					s = new StringBuilder();
					Object bo = param.get(s.append(preKey).append(".").append(key).toString());
					if(bo == null){
						logger.info(s.toString() +"not find value from paramMap");
					}else{
						ReflectionUtil.invokeSet(rule,name,StringUtil.StringToField((String)bo, f));
					}
				}else{
					if(noBase == null && name.equals("base")){
						s = new StringBuilder();
						Object bo = param.get(s.append(preKey).append(".").append("base").toString());
						if(bo == null){
							logger.info(s.toString() +"not find value from paramMap");
						}else{
							ReflectionUtil.invokeSet(rule,name,bo);
						}
					}else if(noParam == null && name.equals("param")){
						s = new StringBuilder();
						Object po = param.get(s.append(preKey).append(".").append("param").toString());
						if(po == null){
							logger.info(s.toString() +"not find value from paramMap");
						}else{
							ReflectionUtil.invokeSet(rule,name,po);
						}
					}
				}
			}
		}
		
	}
	

}
