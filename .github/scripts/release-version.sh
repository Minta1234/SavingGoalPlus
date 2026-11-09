#!/usr/bin/env bash
set -euo pipefail

platform=$1
version_pattern='(0|[1-9][0-9]{0,4})\.(0|[1-9][0-9]?)\.(0|[1-9][0-9]?)'
prefix=v
if [[ "$platform" == ios ]]; then
  prefix=ios-v
fi

if [[ "$GITHUB_REF" == refs/tags/* ]]; then
  tag=${GITHUB_REF#refs/tags/}
  version=${tag#ios-}
  version=${version#v}
elif [[ -n "${INPUT_VERSION:-}" ]]; then
  version=${INPUT_VERSION#v}
  tag="$prefix$version"
else
  # Pick the greatest stable version, including tags on other branches.
  tags=$(git tag --list)
  if [[ "$platform" == android ]]; then
    tags=$(printf '%s\n' "$tags" | sed '/^ios-v/d')
  fi
  versions=$(printf '%s\n' "$tags" | sed -E 's/^(ios-)?v//' | grep -Ex "$version_pattern" || true)
  baseline=$(sed -n 's/^VERSION_NAME=//p' gradle.properties | tr -d '\r')
  current=$(printf '%s\n' "$versions" "$baseline" | sort -t. -k1,1n -k2,2n -k3,3n | tail -n 1)
  if [[ ! "$current" =~ ^$version_pattern$ ]]; then
    echo "::error::Invalid current release version: $current"
    exit 1
  fi
  IFS=. read -r major minor patch <<< "$current"
  case "${BUMP_TYPE:-patch}" in
    major) major=$((major + 1)); minor=0; patch=0 ;;
    minor) minor=$((minor + 1)); patch=0 ;;
    patch) patch=$((patch + 1)) ;;
    *) echo "::error::Invalid bump type"; exit 1 ;;
  esac
  if (( patch == 100 )); then
    patch=0
    minor=$((minor + 1))
  fi
  if (( minor == 100 )); then
    minor=0
    major=$((major + 1))
  fi
  version="$major.$minor.$patch"
  tag="$prefix$version"
fi

if [[ ! "$version" =~ ^$version_pattern$ ]]; then
  echo "::error::Expected a numeric version MAJOR.MINOR.PATCH (minor and patch 0–99)."
  exit 1
fi

if existing=$(git rev-parse --verify "refs/tags/$tag^{commit}" 2>/dev/null) && [[ "$existing" != "$GITHUB_SHA" ]]; then
  echo "::error::Tag $tag already points to another commit."
  exit 1
fi

IFS=. read -r major minor patch <<< "$version"
code=$((major * 10000 + minor * 100 + patch))
if (( code == 0 )); then
  echo "::error::Release version must be greater than 0.0.0."
  exit 1
fi
{
  echo "RELEASE_VERSION=$version"
  echo "RELEASE_TAG=$tag"
  echo "RELEASE_CODE=$code"
} >> "$GITHUB_ENV"
echo "Publishing $tag from $GITHUB_SHA"
