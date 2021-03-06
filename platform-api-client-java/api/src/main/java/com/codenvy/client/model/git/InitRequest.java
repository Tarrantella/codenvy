/*******************************************************************************
 * Copyright (c) [2012] - [2016] Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.client.model.git;

/**
 * Git initial request
 * @author Florent Benoit
 */
public interface InitRequest {

    /**
     * @return the working directory
     */
    String getWorkingDir();

    /**
     * @return true if the git repository should be a bare repository
     */
    boolean isBare();

    /**
     * @return true if it's the initial commit
     */
    boolean isInitCommit();

}
