import { useState, type ReactNode, type SubmitEvent } from 'react'
import { saveBio, saveProfile } from '../api/profile'
import {
  AVAILABILITY,
  ENV_PREFS,
  GOALS,
  LOOKING_FOR,
  type Availability,
  type Bio,
} from '../api/users'
import {
  AVAILABILITY_LABELS,
  ENV_PREF_LABELS,
  GOAL_LABELS,
  LOOKING_FOR_LABELS,
  SKILL_LABELS,
} from '../profile/labels'
import {
  LIMITS,
  validateProfile,
  type ProfileFormErrors,
  type ProfileFormValues,
} from '../profile/validateProfile'

type Props = {
  name: string
  about: string
  bio: Bio | null
  onSaved: () => void
  onCancel?: () => void
}

export function ProfileForm({ name, about, bio, onSaved, onCancel }: Props) {
  const [values, setValues] = useState<ProfileFormValues>(() => initialValues(name, about, bio))
  const [errors, setErrors] = useState<ProfileFormErrors>({})
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [locating, setLocating] = useState(false)

  function set<K extends keyof ProfileFormValues>(key: K, value: ProfileFormValues[K]) {
    setValues((current) => ({ ...current, [key]: value }))
  }

  function toggleAvailability(slot: Availability) {
    set(
      'availability',
      values.availability.includes(slot)
        ? values.availability.filter((s) => s !== slot)
        : [...values.availability, slot],
    )
  }

  function locateMe() {
    if (!('geolocation' in navigator)) {
      setErrors((e) => ({ ...e, location: 'This browser cannot share a location.' }))
      return
    }
    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      (position) => {
        set('location', { lat: position.coords.latitude, lng: position.coords.longitude })
        setErrors((e) => ({ ...e, location: undefined }))
        setLocating(false)
      },
      () => {
        setErrors((e) => ({ ...e, location: 'Could not get your location. Allow access and try again.' }))
        setLocating(false)
      },
    )
  }

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaveError(null)
    const result = validateProfile(values)
    if (!result.ok) {
      setErrors(result.errors)
      return
    }
    setErrors({})
    setSaving(true)
    try {
      await saveProfile(result.profile)
      await saveBio(result.bio)
      onSaved()
    } catch {
      setSaveError('Could not save your profile. Please try again.')
      setSaving(false)
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit} noValidate>
      <fieldset>
        <legend>About you</legend>

        <Field label="Name" error={errors.displayName}>
          <input
            type="text"
            autoComplete="nickname"
            maxLength={LIMITS.nameMax}
            value={values.displayName}
            onChange={(e) => set('displayName', e.target.value)}
          />
        </Field>

        <Field label="About me" error={errors.aboutMe}>
          <textarea
            rows={3}
            maxLength={LIMITS.aboutMax}
            value={values.aboutMe}
            onChange={(e) => set('aboutMe', e.target.value)}
          />
        </Field>

        <Field label="Age" error={errors.age}>
          <input
            type="text"
            inputMode="numeric"
            value={values.age}
            onChange={(e) => set('age', e.target.value)}
          />
        </Field>

        <Field label="Skill level" error={errors.skillLevel}>
          <select value={values.skillLevel} onChange={(e) => set('skillLevel', e.target.value)}>
            <option value="">Choose…</option>
            {SKILL_LABELS.map((label, index) => (
              <option key={label} value={index + 1}>
                {label}
              </option>
            ))}
          </select>
        </Field>

        <Field label="Main goal" error={errors.goal}>
          <select value={values.goal} onChange={(e) => set('goal', e.target.value as ProfileFormValues['goal'])}>
            <option value="">Choose…</option>
            {GOALS.map((goal) => (
              <option key={goal} value={goal}>
                {GOAL_LABELS[goal]}
              </option>
            ))}
          </select>
        </Field>

        <Field label="Indoor or outdoor" error={errors.envPref}>
          <select
            value={values.envPref}
            onChange={(e) => set('envPref', e.target.value as ProfileFormValues['envPref'])}
          >
            <option value="">Choose…</option>
            {ENV_PREFS.map((pref) => (
              <option key={pref} value={pref}>
                {ENV_PREF_LABELS[pref]}
              </option>
            ))}
          </select>
        </Field>

        <div className="field">
          <span id="availability-label">When can you play?</span>
          <div className="checks" role="group" aria-labelledby="availability-label">
            {AVAILABILITY.map((slot) => (
              <label key={slot}>
                <input
                  type="checkbox"
                  checked={values.availability.includes(slot)}
                  onChange={() => toggleAvailability(slot)}
                />
                {AVAILABILITY_LABELS[slot]}
              </label>
            ))}
          </div>
          {errors.availability && <small className="form-error">{errors.availability}</small>}
        </div>
      </fieldset>

      <fieldset>
        <legend>Who you are looking for</legend>

        <Field label="Looking for" error={errors.lookingFor}>
          <select
            value={values.lookingFor}
            onChange={(e) => set('lookingFor', e.target.value as ProfileFormValues['lookingFor'])}
          >
            <option value="">Choose…</option>
            {LOOKING_FOR.map((option) => (
              <option key={option} value={option}>
                {LOOKING_FOR_LABELS[option]}
              </option>
            ))}
          </select>
        </Field>

        <div className="field-row">
          <Field label="Youngest" error={errors.ageMin}>
            <input
              type="text"
              inputMode="numeric"
              value={values.ageMin}
              onChange={(e) => set('ageMin', e.target.value)}
            />
          </Field>
          <Field label="Oldest" error={errors.ageMax}>
            <input
              type="text"
              inputMode="numeric"
              value={values.ageMax}
              onChange={(e) => set('ageMax', e.target.value)}
            />
          </Field>
        </div>
      </fieldset>

      <fieldset>
        <legend>Location</legend>

        <div className="field">
          <span>Where you are</span>
          <div className="location">
            <button type="button" className="button" onClick={locateMe} disabled={locating}>
              {locating ? 'Locating…' : values.location ? 'Update my location' : 'Use my location'}
            </button>
            <small>{values.location ? 'Location set.' : 'Not set yet.'}</small>
          </div>
          {errors.location && <small className="form-error">{errors.location}</small>}
        </div>

        <Field label="Maximum distance (km)" error={errors.maxRadiusKm}>
          <input
            type="text"
            inputMode="numeric"
            value={values.maxRadiusKm}
            onChange={(e) => set('maxRadiusKm', e.target.value)}
          />
        </Field>
      </fieldset>

      {saveError && (
        <p className="form-error" role="alert">
          {saveError}
        </p>
      )}

      <div className="form-actions">
        <button type="submit" className="button button-primary" disabled={saving}>
          Save profile
        </button>
        {onCancel && (
          <button type="button" className="button" onClick={onCancel} disabled={saving}>
            Cancel
          </button>
        )}
      </div>
    </form>
  )
}

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <div className="field">
      {/* the error sits outside the label so it does not become part of the input's name */}
      <label className="field-label">
        <span>{label}</span>
        {children}
      </label>
      {error && <small className="form-error">{error}</small>}
    </div>
  )
}

function initialValues(name: string, about: string, bio: Bio | null): ProfileFormValues {
  return {
    displayName: name,
    aboutMe: about,
    age: bio ? String(bio.age) : '',
    skillLevel: bio ? String(bio.skillLevel) : '',
    goal: bio?.goal ?? '',
    lookingFor: bio?.lookingFor ?? '',
    envPref: bio?.envPref ?? '',
    ageMin: bio ? String(bio.ageMin) : '18',
    ageMax: bio ? String(bio.ageMax) : '',
    availability: bio?.availability ?? [],
    location: bio ? { lat: bio.lat, lng: bio.lng } : null,
    maxRadiusKm: bio ? String(bio.maxRadiusKm) : '25',
  }
}
