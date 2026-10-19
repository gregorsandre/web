import { useEffect, useState, type ReactNode, type SubmitEvent } from 'react'
import { ApiError } from '../api/client'
import { getAllTags, getMyTags, type Tag } from '../api/tags'
import { saveProfileSetup } from '../api/profile'
import {
  AVAILABILITY,
  ENV_PREFS,
  GOALS,
  LOOKING_FOR,
  type Availability,
  type Bio,
  type BioInput,
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
  const [allTags, setAllTags] = useState<Tag[]>([])
  const [selectedTagIds, setSelectedTagIds] = useState<number[]>([])
  const [tagsLoaded, setTagsLoaded] = useState(false)
  const [tagsError, setTagsError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([getAllTags(controller.signal), getMyTags(controller.signal)])
      .then(([all, mine]) => {
        setAllTags(all)
        setSelectedTagIds(mine.map((tag) => tag.id))
        setTagsLoaded(true)
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setTagsError('Could not load the list of sports. Please reload the page.')
        }
      })
    return () => controller.abort()
  }, [])

  function toggleTag(id: number) {
    setTagsError(null)
    setSelectedTagIds((current) =>
      current.includes(id) ? current.filter((t) => t !== id) : [...current, id],
    )
  }

  const tagsByCategory = allTags.reduce<Record<string, Tag[]>>((groups, tag) => {
    ;(groups[tag.category] ??= []).push(tag)
    return groups
  }, {})

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
      (error) => {
        console.error('Geolocation error', error.code, error.message)
        const message =
          error.code === error.PERMISSION_DENIED
            ? 'Location access is blocked. Allow it in the browser and system settings, then try again.'
            : error.code === error.TIMEOUT
              ? 'Finding your location took too long. Try again.'
              : 'Your device could not work out its location. Check that location services are on.'
        setErrors((e) => ({ ...e, location: message }))
        setLocating(false)
      },
      { enableHighAccuracy: false, timeout: 15000, maximumAge: 60000 },
    )
  }

    async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
      event.preventDefault()
      setSaveError(null)
      const result = validateProfile(values)
      const noSports = selectedTagIds.length === 0
      setTagsError(noSports ? 'Choose at least one sport.' : null)
      if (!result.ok) {
        setErrors(result.errors)
        return
      }
      if (noSports) return
      const location = values.location
      if (!location) return
      setErrors({})
      setSaving(true)
      try {
        await saveProfileSetup({
          displayName: values.displayName.trim(),
          aboutMe: values.aboutMe.trim(),
          bio: {
            skillLevel: Number(values.skillLevel),
            goal: values.goal as BioInput['goal'],
            lookingFor: values.lookingFor as BioInput['lookingFor'],
            envPref: values.envPref as BioInput['envPref'],
            age: Number(values.age),
            ageMin: Number(values.ageMin),
            ageMax: Number(values.ageMax),
            lat: location.lat,
            lng: location.lng,
            maxRadiusKm: Number(values.maxRadiusKm),
            availability: values.availability,
          },
          tagIds: selectedTagIds,
        })
        onSaved()
      } catch (err) {
        setSaveError(
          err instanceof ApiError && err.status === 400
            ? err.message
            : 'Could not save your profile. Please try again.',
        )
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

        <fieldset>
          <legend>Your sports</legend>
          {Object.entries(tagsByCategory).map(([category, tags]) => (
            <div className="field" key={category}>
              <span>{category}</span>
              <div className="checks" role="group" aria-label={category}>
                {tags.map((tag) => (
                  <label key={tag.id}>
                    <input
                      type="checkbox"
                      checked={selectedTagIds.includes(tag.id)}
                      onChange={() => toggleTag(tag.id)}
                    />
                    {tag.name}
                  </label>
                ))}
              </div>
            </div>
          ))}
          {tagsError && <small className="form-error">{tagsError}</small>}
        </fieldset>

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
        <button type="submit" className="button button-primary" disabled={saving || !tagsLoaded}>
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
