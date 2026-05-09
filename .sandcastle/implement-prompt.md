# TASK

Fix issue #{{ISSUE_NUMBER}}: {{ISSUE_TITLE}}

Pull in the issue using `gh issue view`, with comments. If it has a parent PRD, pull that in too.

Only work on the issue specified.

Work on branch {{BRANCH}}. Make commits, run tests, and close the issue when done.

# CONTEXT

Here are the last 5 commits:

<recent-commits>

!`git log -n 5 --format="%H %ad %s" --date=short`

</recent-commits>

# EXPLORATION

Read only the files directly relevant to the issue — the affected service, its repository, controller, and existing tests. Do not browse the whole codebase.

# EXECUTION

If applicable, use RGR to complete the task.

1. RED: write one test
2. GREEN: write the implementation to pass that test
3. REPEAT until done
4. REFACTOR the code

# DONE MEANS DONE

As soon as `./mvnw compile -q && ./mvnw test` passes:

1. Commit with message: `[SMS]: <one line summary> (closes #{{ISSUE_NUMBER}})`
2. Output <promise>COMPLETE</promise> immediately.

Do not explore further. Do not refactor. Do not add extra tests. Stop.
