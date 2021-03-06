/*
 * Copyright 2016 EPAM Systems
 * 
 * 
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-api
 * 
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */ 

package com.epam.ta.reportportal.core.widget.content;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.epam.ta.reportportal.commons.Predicates;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.database.StatisticsDocumentHandler;
import com.epam.ta.reportportal.database.dao.LaunchRepository;
import com.epam.ta.reportportal.database.search.Filter;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.widget.ChartObject;
import com.google.common.collect.Lists;

/**
 * Implementation of {@link IContentLoadingStrategy} for line chart
 * 
 * @author Aliaksei_Makayed
 * @author Andrei_Ramanchuk
 */
@Service("LineChartContentLoader")
public class LineChartContentLoader extends StatisticBasedContentLoader implements IContentLoadingStrategy {

	@Autowired
	private LaunchRepository launchRepository;

	@SuppressFBWarnings("NP_NULL_PARAM_DEREF")
	@Override
	public Map<String, List<ChartObject>> loadContent(Filter filter, Sort sorting, int quantity, List<String> contentFields,
			List<String> metaDataFields, Map<String, List<String>> options) {

		BusinessRule.expect(metaDataFields == null || metaDataFields.isEmpty(), Predicates.equalTo(Boolean.FALSE))
				.verify(ErrorType.UNABLE_LOAD_WIDGET_CONTENT, "Metadata fields should exist for providing content.");

		List<String> allFields = Lists.newArrayList(contentFields);
		List<String> xAxis = metaDataFields;
		allFields.addAll(xAxis);
		StatisticsDocumentHandler handler = new StatisticsDocumentHandler(contentFields, xAxis);

		String collectionName = getCollectionName(filter.getTarget());

		// here can be used any repository which extends ReposrtPortalRepository
		launchRepository.loadWithCallback(filter, sorting, quantity, allFields, handler, collectionName);
		if ((options.get("timeline") != null) && (Period.findByName(options.get("timeline").get(0)) != null)) {
			return groupByDate(handler.getResult(), Period.findByName(options.get("timeline").get(0)));
		}
		Map<String, List<ChartObject>> result = new HashMap<>();
		result.put(RESULT, handler.getResult());
		return result;
	}
}
