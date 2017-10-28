/**
 * Copyright (c) 2017 Angelo Zerr and other contributors as
 * indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.ec4j.core.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * An EditorConfig glob. Citing from <a href="http://editorconfig.org/">:
 * <p>
 * Special characters recognized in section names for wildcard matching:
 * <ul>
 * <li>* Matches any string of characters, except path separators (/)</li>
 * <li>** Matches any string of characters</li>
 * <li>? Matches any single character</li>
 * <li>[name] Matches any single character in name</li>
 * <li>[!name] Matches any single character not in name</li>
 * <li>{s1,s2,s3} Matches any of the strings given (separated by commas) (Available since EditorConfig Core 0.11.0)</li>
 * <li>{num1..num2} Matches any integer numbers between num1 and num2, where num1 and num2 can be either positive or
 * negative</li>
 * </ul>
 *
 * @author <a href="mailto:angelo.zerr@gmail.com">Angelo Zerr</a>
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Glob {

    private static final int MAX_GLOB_LENGTH = 4096;
    private final PatternSyntaxException error;
    private final List<int[]> ranges;
    private final Pattern regex;
    private final String source;

    public Glob(String configDirname, String pattern) {
        this.source = pattern;
        ranges = new ArrayList<int[]>();
        if (pattern.length() > MAX_GLOB_LENGTH) {
            this.regex = null;
            this.error = new PatternSyntaxException(
                    "Glob length exceeds the maximal allowed length of " + MAX_GLOB_LENGTH + " characters", pattern,
                    MAX_GLOB_LENGTH);
        } else {
            pattern = pattern.replace(File.separatorChar, '/');
            pattern = pattern.replaceAll("\\\\#", "#");
            pattern = pattern.replaceAll("\\\\;", ";");
            int slashPos = pattern.indexOf('/');
            if (slashPos >= 0) {
                pattern = configDirname + "/" + (slashPos == 0 ? pattern.substring(1) : pattern);
            } else {
                pattern = "**/" + pattern;
            }
            final String regex = RegexpUtils.convertGlobToRegEx(pattern, ranges);
            PatternSyntaxException err = null;
            Pattern pat = null;
            try {
                pat = Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                err = e;
            }
            this.error = err;
            this.regex = pat;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Glob other = (Glob) obj;
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
            return false;
        return true;
    }

    /**
     * @return the {@link PatternSyntaxException} that was thrown when parsing the {@link #source} or {@code null} when
     *         no {@link PatternSyntaxException} was thrown.
     */
    public PatternSyntaxException getError() {
        return error;
    }

    /**
     * @return the glob string out of which this {@link Glob} was constructed
     */
    public String getSource() {
        return source;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        return result;
    }

    public boolean isEmpty() {
        return source.isEmpty();
    }

    public boolean isValid() {
        return getError() == null;
    }

    /**
     * Matches the given slash ({@code /}) separated path against this {@link Glob}.
     *
     * @param filePath
     *            a slash ({@code /}) separated file path to match against this {@link Glob}
     * @return {@code true} if the given {@code filePath} matches; {@code false} otherwise
     */
    public boolean match(String filePath) {
        if (!isValid()) {
            return false;
        }
        final Matcher matcher = regex.matcher(filePath);
        if (matcher.matches()) {
            for (int i = 0; i < matcher.groupCount(); i++) {
                final int[] range = ranges.get(i);
                final String numberString = matcher.group(i + 1);
                if (numberString == null || numberString.startsWith("0")) {
                    return false;
                }
                int number = Integer.parseInt(numberString);
                if (number < range[0] || number > range[1]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * @return the glob string out of which this {@link Glob} was constructed
     */
    @Override
    public String toString() {
        return source;
    }
}
