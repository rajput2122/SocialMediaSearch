# TASK

Open a pull request for each of the following branches. Do NOT merge anything directly.

{{BRANCHES}}

For each branch:

1. Push the branch to origin if not already pushed:
   ```bash
   git push origin <branch>
   ```

2. Open a PR using this exact format:
   ```bash
   gh pr create \
     --title "[SMS] #<issue-number>: <issue-title>" \
     --body "..." \
     --base master \
     --head <branch> \
     --label "sandcastle"
   ```

   PR body must include these sections:
   - **Summary** — what was built and why (2-3 sentences)
   - **Key decisions** — non-obvious choices made during implementation
   - **Files changed** — list of files and what changed in each
   - **Test coverage** — what is tested and what is not
   - **Follow-ups** — anything left out of scope

3. Link the issue in the PR body with `Closes #<number>`.

4. Remove the `sandcastle-ready` label from the issue:
   ```bash
   gh issue edit <number> --remove-label "sandcastle-ready"
   ```

Here are the issues completed:

{{ISSUES}}

Once all PRs are opened, output <promise>COMPLETE</promise>.
