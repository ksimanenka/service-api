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

package com.epam.ta.reportportal.core.launch.impl;

import static com.epam.ta.reportportal.commons.Preconditions.HAS_ANY_MODE;
import static com.epam.ta.reportportal.commons.Predicates.equalTo;
import static com.epam.ta.reportportal.commons.Predicates.not;
import static com.epam.ta.reportportal.commons.Predicates.notNull;
import static com.epam.ta.reportportal.commons.validation.BusinessRule.expect;
import static com.epam.ta.reportportal.commons.validation.Suppliers.formattedSupplier;
import static com.epam.ta.reportportal.database.search.Condition.EQUALS;
import static com.epam.ta.reportportal.ws.model.ErrorType.ACCESS_DENIED;
import static com.epam.ta.reportportal.ws.model.ErrorType.INCORRECT_FILTER_PARAMETERS;
import static com.epam.ta.reportportal.ws.model.ErrorType.LAUNCH_NOT_FOUND;
import static com.epam.ta.reportportal.ws.model.launch.Mode.DEBUG;
import static com.epam.ta.reportportal.ws.model.launch.Mode.DEFAULT;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.epam.ta.reportportal.core.launch.IGetLaunchHandler;
import com.epam.ta.reportportal.core.widget.content.StatisticBasedContentLoader;
import com.epam.ta.reportportal.database.dao.LaunchRepository;
import com.epam.ta.reportportal.database.dao.ProjectRepository;
import com.epam.ta.reportportal.database.entity.Launch;
import com.epam.ta.reportportal.database.entity.Project;
import com.epam.ta.reportportal.database.entity.ProjectRole;
import com.epam.ta.reportportal.database.entity.statistics.ExecutionCounter;
import com.epam.ta.reportportal.database.entity.statistics.IssueCounter;
import com.epam.ta.reportportal.database.search.Filter;
import com.epam.ta.reportportal.database.search.FilterCondition;
import com.epam.ta.reportportal.ws.converter.LaunchResourceAssembler;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.launch.LaunchResource;
import com.epam.ta.reportportal.ws.model.widget.ChartObject;
import com.google.common.base.Preconditions;

/**
 * Default implementation of {@link IGetLaunchHandler}
 * 
 * @author Aliaksei_Makayed
 * @author Andrei_Ramanchuk
 */
@Service
public class GetLaunchHandler extends StatisticBasedContentLoader implements IGetLaunchHandler {

	private ProjectRepository projectRepository;
	private LaunchRepository launchRepository;
	private LaunchResourceAssembler launchResourceAssember;

	@Autowired
	public GetLaunchHandler(LaunchResourceAssembler launchResourceAssembler, LaunchRepository launchRepository) {
		this.launchResourceAssember = Preconditions.checkNotNull(launchResourceAssembler);
		this.launchRepository = Preconditions.checkNotNull(launchRepository);
	}

	@Autowired
	public void setProjectRepository(ProjectRepository projectRepository) {
		this.projectRepository = projectRepository;
	}

	@Override
	public LaunchResource getLaunch(String launchId, String userName, String projectName) {
		Launch launch = validate(launchId, projectName);
		if (launch.getMode() == DEBUG) {
			Project project = projectRepository.findOne(projectName);
			final Project.UserConfig userConfig = project.getUsers().get(userName);
			expect(userConfig.getProjectRole(), not(equalTo(ProjectRole.CUSTOMER))).verify(ACCESS_DENIED);
		}
		return launchResourceAssember.toResource(launch);
	}

	@Override
	public LaunchResource getLaunchByName(String project, Pageable pageable, Filter filter, String username) {
		filter.addCondition(new FilterCondition(EQUALS, false, project, Launch.PROJECT));
		Page<Launch> launches = launchRepository.findByFilter(filter, pageable);
		expect(launches, notNull()).verify(LAUNCH_NOT_FOUND);
		return launchResourceAssember.toResource(launches.iterator().next());
	}

	@Override
	public Iterable<LaunchResource> getProjectLaunches(String projectName, Filter filter, Pageable pageable, String userName) {
		/*
		 * input filter shouldn't contains any "mode" related filter conditions
		 * "debug mode" conditions are forbidden because user can manipulate
		 * with "debug mode" input filter and see not own debug launches
		 * 'default mode' conditions are forbidden because 'default mode'
		 * condition will be added by server(line 66) Due to limitations of the
		 * com.mongodb.BasicDBObject, user can't add a second 'mode' criteria
		 */
		validateModeConditions(filter);
		filter.addCondition(new FilterCondition(EQUALS, false, projectName, Launch.PROJECT));
		// add condition for loading only launches with default mode
		filter.addCondition(new FilterCondition(EQUALS, false, DEFAULT.toString(), Launch.MODE_CRITERIA));
		Page<Launch> launches = launchRepository.findByFilter(filter, pageable);
		return launchResourceAssember.toPagedResources(launches);
	}

	/*
	 * Changed logic for this method: It should return DEBUG launches for
	 * project users, for specified user or only owner
	 */
	@Override
	public Iterable<LaunchResource> getDebugLaunches(String projectName, String userName, Filter filter, Pageable pageable) {
		filter.addCondition(new FilterCondition(EQUALS, false, projectName, Launch.PROJECT));
		filter.addCondition(new FilterCondition(EQUALS, false, DEBUG.toString(), Launch.MODE_CRITERIA));
		Page<Launch> launches = launchRepository.findByFilter(filter, pageable);
		return launchResourceAssember.toPagedResources(launches);
	}

	@Override
	public List<String> getTags(String project, String value) {
		return launchRepository.findDistinctValues(project, value, "tags");
	}

	@Override
	public List<String> getLaunchNames(String project, String value) {
		expect(value.length() > 2, equalTo(true)).verify(INCORRECT_FILTER_PARAMETERS,
				formattedSupplier("Length of the launch name string '{}' is less than 3 symbols", value));
		return launchRepository.findValuesWithMode(project, value, "name", DEFAULT.name());
	}

	@Override
	public List<String> getOwners(String project, String value, String field, String mode) {
		expect(value.length() > 2, equalTo(true)).verify(INCORRECT_FILTER_PARAMETERS,
				formattedSupplier("Length of the filtering string '{}' is less than 3 symbols", value));
		return launchRepository.findValuesWithMode(project, value, field, mode);
	}

	@Override
	public Map<String, List<ChartObject>> getLaunchesComparisonInfo(String projectName, String[] ids) {
		Map<String, List<ChartObject>> result = new LinkedHashMap<>();
		List<ChartObject> objects = new ArrayList<>();
		List<Launch> launches = launchRepository.find(Arrays.asList(ids));
		for (Launch launch : launches) {
			ChartObject object = new ChartObject();
			object.setName(launch.getName());
			object.setStartTime(String.valueOf(launch.getStartTime().getTime()));
			object.setNumber(String.valueOf(launch.getNumber()));
			object.setId(launch.getId());

			HashMap<String, Integer> issuesData = new HashMap<>();
			IssueCounter issueCounter = launch.getStatistics().getIssueCounter();
			issuesData.put(getProductBugFieldName().replaceAll("\\.", "\\$"), issueCounter.getProductBugTotal());
			issuesData.put(getSystemIssueFieldName().replaceAll("\\.", "\\$"), issueCounter.getSystemIssueTotal());
			issuesData.put(getAutomationBugFieldName().replaceAll("\\.", "\\$"), issueCounter.getAutomationBugTotal());
			issuesData.put(getToInvestigateFieldName().replaceAll("\\.", "\\$"), issueCounter.getToInvestigateTotal());

			ExecutionCounter executionCounter = launch.getStatistics().getExecutionCounter();
			HashMap<String, Integer> executionData = new HashMap<>();
			executionData.put(getFailedFieldName().replaceAll("\\.", "\\$"), executionCounter.getFailed());
			executionData.put(getPassedFieldName().replaceAll("\\.", "\\$"), executionCounter.getPassed());
			executionData.put(getSkippedFieldName().replaceAll("\\.", "\\$"), executionCounter.getSkipped());

			Map<String, String> computedStatistics = computeFraction(issuesData);
			computedStatistics.putAll(computeFraction(executionData));
			object.setValues(computedStatistics);
			objects.add(object);
		}
		result.put(RESULT, objects);
		return result;
	}

	@Override
	public Map<String, String> getStatuses(String projectName, String[] ids) {
		return launchRepository.find(Arrays.asList(ids)).stream().filter(launch -> launch.getProjectRef().equals(projectName))
				.collect(Collectors.toMap(Launch::getId, launch -> launch.getStatus().toString()));
	}

	private Map<String, String> computeFraction(Map<String, Integer> data) {
		Map<String, String> result = new HashMap<>();
		DecimalFormat formatter = new DecimalFormat("###.##");
		int total = data.values().stream().mapToInt(Integer::intValue).sum();
		String lastKey = null;
		Double lastValue = 0.0;
		Double sum = 0.0;
		Set<Map.Entry<String, Integer>> entries = data.entrySet();
		for (Map.Entry<String, Integer> entry : entries) {
			String formattedValue = formatter.format(total != 0 ? (entry.getValue() / (double) total * 100) : 0);
			lastKey = entry.getKey();
			lastValue = Double.valueOf(formattedValue);
			sum += lastValue;
			result.put(entry.getKey(), formattedValue);
		}
		if (total != 0) {
			result.put(lastKey, formatter.format(100 - (sum - lastValue)));
		}

		return result;
	}

	/**
	 * Validate if filter doesn't contain any "mode" related conditions.
	 * 
	 * @param filter
	 */
	private void validateModeConditions(Filter filter) {
		expect(filter.getFilterConditions().stream().filter(HAS_ANY_MODE).findFirst().isPresent(), equalTo(false))
				.verify(INCORRECT_FILTER_PARAMETERS, "Filters for 'mode' aren't applicable for project's launches.");
	}

	/**
	 * Validate launch reference to specified project ID
	 * 
	 * @param launchId
	 *            - validating launch ID
	 * @param projectName
	 *            - specified project name
	 * @return Launch - validated launch object if not BusinessRule exceptions
	 */
	private Launch validate(String launchId, String projectName) {
		Launch launch = launchRepository.findOne(launchId);
		expect(launch, notNull()).verify(LAUNCH_NOT_FOUND, launchId);

		expect(launch.getProjectRef(), equalTo(projectName)).verify(ErrorType.FORBIDDEN_OPERATION,
				formattedSupplier("Specified launch with id '{}' not referenced to specified project '{}'", launchId, projectName));

		return launch;
	}
}
