/*
 * Copyright 2016 EPAM Systems
 * 
 * 
 * This file is part of EPAM Report Portal.
 * https://github.com/epam/ReportPortal
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

package com.epam.ta.reportportal.database.fixture;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Mongo Fixture Importer. Imports fixtures into mongo db
 * 
 * @author Andrei Varabyeu
 * 
 */
public class MongoFixtureImporter implements FixtureImporter {

	@Autowired
	private MongoTemplate mongoOperations;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.epam.ta.reportportal.database.fixture.FixtureImporter#importFixture
	 * (java.lang.Object)
	 */
	@Override
	public void importFixture(Object fixture) {
		if (fixture instanceof Collection) {
			for (Object object : ((Collection<?>) fixture)) {
				mongoOperations.save(object);
			}
		} else {
			mongoOperations.save(fixture);
		}
	}

	public void dropDatabase() {
		mongoOperations.getDb().dropDatabase();
	}

}