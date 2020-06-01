/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2020 aoju.org Greg Messner and other contributors.         *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 ********************************************************************************/
package org.aoju.bus.gitlab.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.aoju.bus.gitlab.Constants;
import org.aoju.bus.gitlab.GitLabApiForm;

/**
 * This class is used to filter commit status when getting lists of them.
 *
 * @author Kimi Liu
 * @version 5.9.6
 * @since JDK 1.8+
 */
public class CommitStatusFilter {

    private String ref;
    private String stage;
    private String name;
    private Boolean all;

    public CommitStatusFilter withRef(String ref) {
        this.ref = ref;
        return this;
    }

    public CommitStatusFilter withStage(String stage) {
        this.stage = stage;
        return this;
    }

    public CommitStatusFilter withName(String name) {
        this.name = name;
        return this;
    }


    public CommitStatusFilter withAll(Boolean all) {
        this.all = all;
        return this;
    }

    @JsonIgnore
    public GitLabApiForm getQueryParams(int page, int perPage) {
        return (getQueryParams()
                .withParam(Constants.PAGE_PARAM, page)
                .withParam(Constants.PER_PAGE_PARAM, perPage));
    }

    @JsonIgnore
    public GitLabApiForm getQueryParams() {
        return (new GitLabApiForm()
                .withParam("ref", ref)
                .withParam("stage", stage)
                .withParam("name", name)
                .withParam("all", all));
    }
}
