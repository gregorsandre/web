import type { Bio } from '../api/users'
import {
  AVAILABILITY_LABELS,
  ENV_PREF_LABELS,
  GOAL_LABELS,
  LOOKING_FOR_LABELS,
  skillLabel,
} from '../profile/labels'

type Props = {
  bio: Bio
  // The age range someone is looking for is private: only shown on your own profile
  showAgeRange?: boolean
}

// Coordinates are deliberately not shown anywhere
export function BioDetails({ bio, showAgeRange = false }: Props) {
  return (
    <dl className="bio">
      <dt>Age</dt>
      <dd>{bio.age}</dd>
      <dt>Skill level</dt>
      <dd>{skillLabel(bio.skillLevel)}</dd>
      <dt>Goal</dt>
      <dd>{GOAL_LABELS[bio.goal]}</dd>
      <dt>Looking for</dt>
      <dd>
        {LOOKING_FOR_LABELS[bio.lookingFor]}
        {showAgeRange && `, aged ${bio.ageMin}–${bio.ageMax}`}
      </dd>
      <dt>Prefers</dt>
      <dd>{ENV_PREF_LABELS[bio.envPref]}</dd>
      <dt>Available</dt>
      <dd>{bio.availability.map((slot) => AVAILABILITY_LABELS[slot]).join(', ')}</dd>
    </dl>
  )
}

export function BioTags({ bio }: { bio: Bio }) {
  return (
    <ul className="tags">
      <li>{bio.age} y</li>
      <li>{skillLabel(bio.skillLevel)}</li>
      <li>{GOAL_LABELS[bio.goal]}</li>
      <li>{ENV_PREF_LABELS[bio.envPref]}</li>
    </ul>
  )
}
