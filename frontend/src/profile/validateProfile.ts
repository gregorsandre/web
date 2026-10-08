import type { ProfileInput } from '../api/profile'
import type { Availability, BioInput, EnvPref, Goal, LookingFor } from '../api/users'

// Text inputs hold strings, so the form state is kept as typed and only turned
// into numbers here.
export type ProfileFormValues = {
  displayName: string
  aboutMe: string
  age: string
  skillLevel: string
  goal: Goal | ''
  lookingFor: LookingFor | ''
  envPref: EnvPref | ''
  ageMin: string
  ageMax: string
  availability: Availability[]
  location: { lat: number; lng: number } | null
  maxRadiusKm: string
}

export type ProfileFormErrors = Partial<Record<keyof ProfileFormValues, string>>

export type ValidationResult =
  | { ok: true; profile: ProfileInput; bio: BioInput }
  | { ok: false; errors: ProfileFormErrors }

// Limits mirror the CHECK constraints in V3__profiles_bios.sql
export const LIMITS = {
  nameMax: 50,
  aboutMax: 500,
  ageMin: 18,
  ageMax: 120,
  radiusMin: 1,
  radiusMax: 500,
}

export function validateProfile(values: ProfileFormValues): ValidationResult {
  const errors: ProfileFormErrors = {}

  const displayName = values.displayName.trim()
  if (!displayName) errors.displayName = 'Enter a name.'

  const age = wholeNumber(values.age)
  if (age === null || age < LIMITS.ageMin || age > LIMITS.ageMax) {
    errors.age = `Enter an age between ${LIMITS.ageMin} and ${LIMITS.ageMax}.`
  }

  const skillLevel = wholeNumber(values.skillLevel)
  if (skillLevel === null || skillLevel < 1 || skillLevel > 5) {
    errors.skillLevel = 'Pick a skill level.'
  }

  if (!values.goal) errors.goal = 'Pick a goal.'
  if (!values.lookingFor) errors.lookingFor = 'Pick what you are looking for.'
  if (!values.envPref) errors.envPref = 'Pick a preference.'

  const ageMin = wholeNumber(values.ageMin)
  const ageMax = wholeNumber(values.ageMax)
  if (ageMin === null || ageMin < LIMITS.ageMin || ageMin > LIMITS.ageMax) {
    errors.ageMin = `Enter a minimum age between ${LIMITS.ageMin} and ${LIMITS.ageMax}.`
  }
  if (ageMax === null || ageMax < LIMITS.ageMin || ageMax > LIMITS.ageMax) {
    errors.ageMax = `Enter a maximum age between ${LIMITS.ageMin} and ${LIMITS.ageMax}.`
  } else if (ageMin !== null && !errors.ageMin && ageMin > ageMax) {
    errors.ageMax = 'The maximum age cannot be below the minimum.'
  }

  if (values.availability.length === 0) errors.availability = 'Pick at least one time.'
  if (!values.location) errors.location = 'Set your location.'

  const maxRadiusKm = wholeNumber(values.maxRadiusKm)
  if (maxRadiusKm === null || maxRadiusKm < LIMITS.radiusMin || maxRadiusKm > LIMITS.radiusMax) {
    errors.maxRadiusKm = `Enter a distance between ${LIMITS.radiusMin} and ${LIMITS.radiusMax} km.`
  }

  if (
    Object.keys(errors).length > 0 ||
    age === null ||
    skillLevel === null ||
    ageMin === null ||
    ageMax === null ||
    maxRadiusKm === null ||
    !values.goal ||
    !values.lookingFor ||
    !values.envPref ||
    !values.location
  ) {
    return { ok: false, errors }
  }

  return {
    ok: true,
    profile: { displayName, aboutMe: values.aboutMe.trim() },
    bio: {
      skillLevel,
      goal: values.goal,
      lookingFor: values.lookingFor,
      envPref: values.envPref,
      age,
      ageMin,
      ageMax,
      lat: values.location.lat,
      lng: values.location.lng,
      maxRadiusKm,
      availability: values.availability,
    },
  }
}

function wholeNumber(text: string): number | null {
  return /^\d+$/.test(text.trim()) ? Number(text.trim()) : null
}
