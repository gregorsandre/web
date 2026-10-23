import type { Bio } from '../api/users'
import type { Tag } from '../api/tags'
import {
  AVAILABILITY_LABELS,
  ENV_PREF_LABELS,
  GOAL_LABELS,
  LOOKING_FOR_LABELS,
  skillLabel,
} from '../profile/labels'

export type BioFields = Pick<
  Bio, 'age' | 'skillLevel' | 'goal' | 'lookingFor' | 'envPref' | 'availability'> &
  Partial<Pick<Bio, 'ageMin' | 'ageMax'>>

type Props = {
  bio: BioFields
  tags?: Tag[]
  // The age range someone is looking for is private: only shown on your own profile
  showAgeRange?: boolean
}

// Coordinates are deliberately not shown anywhere
export function BioDetails({ bio, tags, showAgeRange = false }: Props) {
  const hasAgeRange = bio.ageMin !== undefined && bio.ageMax !== undefined
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
        {showAgeRange && hasAgeRange && `, aged ${bio.ageMin}–${bio.ageMax}`}
      </dd>
      <dt>Prefers</dt>
      <dd>{ENV_PREF_LABELS[bio.envPref]}</dd>
      <dt>Available</dt>
      <dd>{bio.availability.map((slot) => AVAILABILITY_LABELS[slot]).join(', ')}</dd>
      {tags && tags.length > 0 && (
              <>
                <dt>Sports</dt>
                <dd>{tags.map((tag) => tag.name).join(', ')}</dd>
              </>
      )}
    </dl>
  )
}

export function BioTags({ bio, sports = [] }: { bio: BioFields; sports?: Tag[] }) {
  return (
    <ul className="tags">
      <li>{bio.age} y</li>
      <li>{skillLabel(bio.skillLevel)}</li>
      <li>{GOAL_LABELS[bio.goal]}</li>
      <li>{ENV_PREF_LABELS[bio.envPref]}</li>
      {sports.map((sport) => (
        <li key={sport.id} className="tag-sport">
          {sport.name}
        </li>
      ))}
    </ul>
  )
}
