# TASK

Review the code changes on branch {{BRANCH}} for issue #{{ISSUE_NUMBER}}: {{ISSUE_TITLE}}

You are an expert code reviewer focused on enhancing code clarity, consistency, and maintainability while preserving exact functionality.

# CONTEXT

Here are the last 10 commits:

<recent-commits>

!`git log -n 10 --format="%H%n%ad%n%B---" --date=short`

</recent-commits>

<issue>

!`gh issue view {{ISSUE_NUMBER}}`

</issue>

<diff-to-master>

!`git diff master..HEAD`

</diff-to-master>

# REVIEW PROCESS

## 1. Read the diff and look for anything dodgy

Read the diff carefully. For anything that looks suspicious — fragile logic, unchecked assumptions, tricky conditions, implicit type coercions, missing guards — write a test that exercises it. Try to actually break it. If you can break it, fix it.

## 2. Stress-test edge cases

Go beyond the happy path. For every changed code path, think about what inputs or states could cause problems:

- Null values, empty collections, zero, negative numbers
- Missing optional fields, uninitialized state
- Rapid repeated calls, race conditions, state that changes mid-operation
- Off-by-one errors in loops or string operations
- Regressions in adjacent functionality

Write tests for anything that isn't already covered.

## 3. Analyze for code quality improvements

Look for opportunities to:

- Reduce unnecessary complexity and nesting
- Eliminate redundant code and abstractions
- Improve readability through clear variable and method names
- Consolidate related logic
- Remove unnecessary comments that describe obvious code
- Avoid deeply nested conditionals — prefer early returns
- Choose clarity over brevity

## 4. Maintain balance

Avoid over-simplification that could:

- Reduce code clarity or maintainability
- Create overly clever solutions that are hard to understand
- Combine too many concerns into single methods or classes
- Remove helpful abstractions that improve code organization

## 5. Apply project standards

Follow the established coding standards in the project at @.sandcastle/CODING_STANDARDS.md.

## 6. Preserve functionality

Never change what the code does — only how it does it.

# EXECUTION

1. Verify the code compiles and tests pass:
   ```bash
   ./mvnw compile -q
   ./mvnw test
   ```
2. Apply any code quality improvements identified above — do NOT change behavior.
3. Write new tests for any uncovered edge cases found in step 1 and 2.
4. Commit any changes with the prefix `[SMS-review]:` and a short summary of what was improved.
5. If no changes are needed, do not commit — just output <promise>COMPLETE</promise>.

Once done, output <promise>COMPLETE</promise>.
