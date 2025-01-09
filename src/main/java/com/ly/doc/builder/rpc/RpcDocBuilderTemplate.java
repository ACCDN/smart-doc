/*
 *
 * Copyright (C) 2018-2024 smart-doc
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ly.doc.builder.rpc;

import com.ly.doc.builder.IRpcDocBuilderTemplate;
import com.ly.doc.constants.DocGlobalConstants;
import com.ly.doc.constants.FrameworkEnum;
import com.ly.doc.constants.TornaConstants;
import com.ly.doc.model.*;
import com.ly.doc.model.rpc.RpcApiDoc;
import com.ly.doc.utils.DocPathUtil;
import com.ly.doc.utils.DocUtil;
import com.power.common.util.CollectionUtil;
import com.power.common.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * rpc doc builder template.
 *
 * @author yu 2020/5/16.
 */
public class RpcDocBuilderTemplate implements IRpcDocBuilderTemplate<RpcApiDoc> {

	@Override
	public void checkAndInit(ApiConfig config, boolean checkOutPath) {
		if (StringUtil.isEmpty(config.getFramework())) {
			config.setFramework(FrameworkEnum.DUBBO.getFramework());
		}
		IRpcDocBuilderTemplate.super.checkAndInit(config, checkOutPath);
		config.setOutPath(config.getOutPath() + DocGlobalConstants.FILE_SEPARATOR + DocGlobalConstants.RPC_OUT_DIR);
	}

	@Override
	public RpcApiDoc createEmptyApiDoc() {
		return new RpcApiDoc();
	}

	/**
	 * handle group api docs.
	 * @param apiDocList list of apiDocList
	 * @param apiConfig ApiConfig apiConfig
	 * @return List of ApiDoc
	 * @author wangaiping
	 */

	List<RpcApiDoc> handleApiGroup(List<RpcApiDoc> apiDocList, ApiConfig apiConfig) {
		if (CollectionUtil.isEmpty(apiDocList) || apiConfig == null) {
			return apiDocList;
		}
		List<ApiGroup> groups = apiConfig.getGroups();
		List<RpcApiDoc> finalApiDocs = new ArrayList<>();

		RpcApiDoc defaultGroup = RpcApiDoc.buildGroupApiDoc(TornaConstants.DEFAULT_GROUP_CODE);
		// show default group
		AtomicInteger order = new AtomicInteger(1);
		finalApiDocs.add(defaultGroup);

		if (CollectionUtil.isEmpty(groups)) {
			defaultGroup.setOrder(order.getAndIncrement());
			defaultGroup.getChildrenApiDocs().addAll(apiDocList);
			return finalApiDocs;
		}
		Map<String, String> hasInsert = new HashMap<>(16);
		for (ApiGroup group : groups) {
			RpcApiDoc groupApiDoc = RpcApiDoc.buildGroupApiDoc(group.getName());
			finalApiDocs.add(groupApiDoc);
			for (RpcApiDoc doc : apiDocList) {
				if (hasInsert.containsKey(doc.getAlias())) {
					continue;
				}
				if (!DocUtil.isMatch(group.getApis(), doc.getPackageName() + "." + doc.getName())) {
					continue;
				}
				hasInsert.put(doc.getAlias(), null);
				groupApiDoc.getChildrenApiDocs().add(doc);
				doc.setOrder(groupApiDoc.getChildrenApiDocs().size());
				doc.setGroup(group.getName());
				if (StringUtil.isEmpty(group.getPaths())) {
					continue;
				}
				List<RpcJavaMethod> methodDocs = doc.getList()
					.stream()
					.filter(l -> DocPathUtil.matches(l.getMethodDefinition(), group.getPaths(), null))
					.collect(Collectors.toList());
				doc.setList(methodDocs);
			}
		}
		// Ungrouped join the default group
		for (RpcApiDoc doc : apiDocList) {
			String key = doc.getAlias();
			if (!hasInsert.containsKey(key)) {
				defaultGroup.getChildrenApiDocs().add(doc);
				doc.setOrder(defaultGroup.getChildrenApiDocs().size());
				hasInsert.put(doc.getAlias(), null);
			}
		}
		if (CollectionUtil.isEmpty(defaultGroup.getChildrenApiDocs())) {
			finalApiDocs.remove(defaultGroup);
		}
		finalApiDocs.forEach(group -> group.setOrder(order.getAndIncrement()));
		return finalApiDocs;
	}

}
