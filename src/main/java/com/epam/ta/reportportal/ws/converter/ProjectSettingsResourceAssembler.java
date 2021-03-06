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

package com.epam.ta.reportportal.ws.converter;

import javax.inject.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Service;

import com.epam.ta.reportportal.database.entity.Project;
import com.epam.ta.reportportal.ws.controller.impl.ProjectSettingsController;
import com.epam.ta.reportportal.ws.converter.builders.ProjectSettingsResourceBuilder;
import com.epam.ta.reportportal.ws.model.project.config.ProjectSettingsResource;

/**
 * @author Andrei_Ramanchuk
 */
@Service
public class ProjectSettingsResourceAssembler extends ResourceAssemblerSupport<Project, ProjectSettingsResource> {

	@Autowired
	private Provider<ProjectSettingsResourceBuilder> builder;

	public ProjectSettingsResourceAssembler() {
		super(ProjectSettingsController.class, ProjectSettingsResource.class);
	}

	@Override
	public ProjectSettingsResource toResource(Project entity) {
		return builder.get().addProjectSettings(entity)
				.addLink(ControllerLinkBuilder.linkTo(ProjectSettingsController.class, entity.getId()).withSelfRel()).build();
	}
}