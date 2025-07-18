#!/bin/bash
# Script to check commit message format
# Enforces conventional commit format: type(scope): message

# Get the commit message file
commit_msg_file=$1
commit_msg=$(cat "$commit_msg_file")

# Define the regex pattern for conventional commits
# Format: type(scope): message
# Where type is one of: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert
pattern='^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([a-z0-9-]+\))?: .{1,100}$'

# Check if the commit message matches the pattern
if ! [[ "$commit_msg" =~ $pattern ]]; then
  echo "ERROR: Commit message does not follow the conventional commit format."
  echo "Required format: type(scope): message"
  echo "Where type is one of: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert"
  echo "Example: feat(auth): add login functionality"
  echo ""
  echo "Your commit message was:"
  echo "$commit_msg"
  exit 1
fi

# Check if the message starts with a capital letter
message_part=$(echo "$commit_msg" | sed -E 's/^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([a-z0-9-]+\))?: //')
first_char=$(echo "$message_part" | cut -c1)

if ! [[ "$first_char" =~ [A-Z] ]]; then
  echo "ERROR: Commit message should start with a capital letter."
  echo "Your message part: $message_part"
  exit 1
fi

# Check if the message ends with a period
last_char=$(echo "$message_part" | rev | cut -c1)
if [[ "$last_char" == "." ]]; then
  echo "ERROR: Commit message should not end with a period."
  echo "Your message part: $message_part"
  exit 1
fi

# All checks passed
exit 0